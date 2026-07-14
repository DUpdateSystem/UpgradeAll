extern crate jni;

use getter::operations::autogen::{self, AutogenAcceptance, AutogenOperationError};
use getter::operations::fdroid_autogen;
use getter::operations::fdroid_catalog::{self, FdroidEndpointConfig};
use getter::operations::github_autogen;
use getter::operations::legacy_room::{self, LegacyRoomOperationError};
use getter::operations::provider_cache::{ProviderCacheMode, ProviderCacheSource};
use getter::operations::read_model::{self, ReadModelOperationError};
use getter::operations::runtime as runtime_operations;
use getter::operations::startup as startup_operations;
use getter::rpc::server::run_server_hanging;
#[cfg(target_os = "android")]
use getter::rustls_platform_verifier;
use jni::objects::{JObject, JString, JValue};
use jni::JNIEnv;
use serde::Deserialize;
use serde_json::{json, Value};
use std::collections::VecDeque;
use std::path::{Path, PathBuf};
use std::sync::mpsc::channel;
use std::sync::{Mutex, OnceLock};
use std::thread;
use upgradeall_platform_adapter::InstalledInventoryScanOptions;
#[cfg(target_os = "android")]
use upgradeall_platform_adapter::PlatformAdapter;

const MAIN_DB_FILE: &str = "main.db";
const CACHE_DB_FILE: &str = "cache.db";
const MAX_RUNTIME_NOTIFICATION_QUEUE: usize = 64;

static GETTER_RUNTIME: OnceLock<Mutex<getter::core::runtime::GetterRuntime>> = OnceLock::new();
static RUNTIME_NOTIFICATIONS: OnceLock<Mutex<VecDeque<Value>>> = OnceLock::new();

#[derive(Debug, Deserialize)]
struct PreviewInstalledAutogenRequest {
    data_dir: PathBuf,
    #[serde(default)]
    scan_options: InstalledInventoryScanOptions,
}

#[derive(Debug, Deserialize)]
struct ApplyInstalledAutogenRequest {
    data_dir: PathBuf,
    preview: Value,
    #[serde(default)]
    acceptance: ApplyAutogenAcceptance,
}

#[derive(Debug, Deserialize)]
struct PreviewFdroidAutogenRequest {
    data_dir: PathBuf,
    #[serde(default)]
    payload: Value,
}

#[derive(Debug, Deserialize)]
#[serde(deny_unknown_fields)]
struct RefreshDefaultFdroidCatalogCacheRequest {
    data_dir: PathBuf,
}

#[derive(Debug, Deserialize)]
struct ApplyFdroidAutogenRequest {
    data_dir: PathBuf,
    preview: Value,
    #[serde(default)]
    acceptance: ApplyAutogenAcceptance,
}

#[derive(Debug, Deserialize)]
#[serde(deny_unknown_fields)]
struct PreviewGithubAutogenRequest {
    data_dir: PathBuf,
    owner: String,
    repo: String,
    android_package: String,
    #[serde(default)]
    display_name: Option<String>,
}

#[derive(Debug, Deserialize)]
struct ApplyGithubAutogenRequest {
    data_dir: PathBuf,
    preview: Value,
    #[serde(default)]
    acceptance: ApplyAutogenAcceptance,
}

#[derive(Debug, Deserialize)]
struct ImportLegacyRoomDatabaseRequest {
    data_dir: PathBuf,
    database_path: PathBuf,
}

#[derive(Debug, Deserialize)]
struct LegacyReportListRequest {
    data_dir: PathBuf,
}

#[derive(Debug, Deserialize)]
#[serde(deny_unknown_fields)]
struct StartupRequest {
    data_dir: PathBuf,
    #[serde(default)]
    scan_options: InstalledInventoryScanOptions,
}

#[derive(Debug, Deserialize)]
struct ReadOperationRequest {
    data_dir: PathBuf,
    operation: String,
    #[serde(default)]
    payload: Value,
}

#[derive(Debug, Deserialize)]
struct RuntimeOperationRequest {
    operation: String,
    #[serde(default)]
    payload: Value,
    #[serde(default)]
    data_dir: Option<PathBuf>,
}

#[derive(Debug, Default, Deserialize)]
struct ApplyAutogenAcceptance {
    #[serde(default)]
    mode: Option<String>,
    #[serde(default)]
    package_ids: Vec<getter::core::PackageId>,
}

#[no_mangle]
pub extern "C" fn Java_net_xzos_upgradeall_getter_NativeLib_runServer<'local>(
    mut env: JNIEnv<'local>,
    _: JObject<'local>,
    context: JObject<'local>,
    callback: JObject<'local>,
) -> JString<'local> {
    if let Err(error) = init_android_integrations(&mut env, &context) {
        return java_string_or_fallback(&mut env, error);
    }

    let (startup_tx, startup_rx) = channel::<Result<String, String>>();
    thread::spawn(move || {
        let runtime = match tokio::runtime::Runtime::new() {
            Ok(rt) => rt,
            Err(e) => {
                let _ = startup_tx.send(Err(format!("Error creating Tokio runtime: {}", e)));
                return;
            }
        };
        runtime.block_on(async move {
            let address = "127.0.0.1:0";
            let startup_error_tx = startup_tx.clone();
            if let Err(e) = run_server_hanging(address, move |url| {
                startup_tx
                    .send(Ok(url.to_string()))
                    .map_err(|_| getter::rpc::server::RpcServerError::StartupCallback)?;
                Ok(())
            })
            .await
            {
                // If startup failed before the URL callback, report it to JNI.
                // If startup succeeded, NativeLib.runServer has already returned
                // to Kotlin and the placeholder server intentionally lives for
                // the lifetime of this background thread.
                let _ = startup_error_tx.send(Err(format!("Error running server: {}", e)));
            }
        });
    });
    let url = match startup_rx.recv() {
        Ok(Ok(url)) => url,
        Ok(Err(error)) => {
            return java_string_or_fallback(&mut env, error);
        }
        Err(e) => {
            return java_string_or_fallback(
                &mut env,
                format!("Error receiving URL from server thread: {}", e),
            );
        }
    };
    let jurl = match env.new_string(url) {
        Ok(jurl) => jurl,
        Err(e) => {
            return java_string_or_fallback(
                &mut env,
                format!("Error creating URL Java string: {e}"),
            );
        }
    };
    let call_result = env.call_method(
        callback,
        "callback",
        "(Ljava/lang/String;)V",
        &[JValue::Object(&jurl)],
    );

    if let Err(e) = call_result {
        return java_string_or_fallback(&mut env, format!("JNI call error: {e}"));
    }

    java_string_or_fallback(&mut env, "")
}

#[no_mangle]
pub extern "C" fn Java_net_xzos_upgradeall_getter_NativeLib_initializeBridge<'local>(
    mut env: JNIEnv<'local>,
    _: JObject<'local>,
    context: JObject<'local>,
) -> JString<'local> {
    let response = match init_android_integrations(&mut env, &context) {
        Ok(()) => {
            init_getter_runtime();
            success_envelope("bridge initialize", json!({ "initialized": true }))
        }
        Err(error) => error_envelope(
            "bridge initialize",
            "bridge.initialize_error",
            "Getter native bridge initialization failed",
            Some(error),
        ),
    };
    java_string_or_fallback(&mut env, response)
}

#[no_mangle]
pub extern "C" fn Java_net_xzos_upgradeall_getter_NativeLib_previewInstalledAutogen<'local>(
    mut env: JNIEnv<'local>,
    _: JObject<'local>,
    context: JObject<'local>,
    request_json: JString<'local>,
) -> JString<'local> {
    let command = "autogen installed preview";
    let response = match jstring_to_string(&mut env, &request_json)
        .and_then(|raw| preview_installed_autogen(&mut env, &context, &raw))
    {
        Ok(data) => success_envelope(command, data),
        Err(error) => operation_error_envelope(command, error),
    };
    java_string_or_fallback(&mut env, response)
}

#[no_mangle]
pub extern "C" fn Java_net_xzos_upgradeall_getter_NativeLib_previewInstalledFdroidAutogen<
    'local,
>(
    mut env: JNIEnv<'local>,
    _: JObject<'local>,
    context: JObject<'local>,
    request_json: JString<'local>,
) -> JString<'local> {
    let command = "autogen installed fdroid preview";
    let response = match jstring_to_string(&mut env, &request_json)
        .and_then(|raw| preview_installed_fdroid_autogen(&mut env, &context, &raw))
    {
        Ok(data) => success_envelope(command, data),
        Err(error) => operation_error_envelope(command, error),
    };
    java_string_or_fallback(&mut env, response)
}

#[no_mangle]
pub extern "C" fn Java_net_xzos_upgradeall_getter_NativeLib_applyInstalledAutogen<'local>(
    mut env: JNIEnv<'local>,
    _: JObject<'local>,
    request_json: JString<'local>,
) -> JString<'local> {
    let command = "autogen installed apply";
    let response = match jstring_to_string(&mut env, &request_json)
        .and_then(|raw| apply_installed_autogen(&raw))
    {
        Ok(data) => success_envelope(command, data),
        Err(error) => operation_error_envelope(command, error),
    };
    java_string_or_fallback(&mut env, response)
}

#[no_mangle]
pub extern "C" fn Java_net_xzos_upgradeall_getter_NativeLib_applyInstalledFdroidAutogen<'local>(
    mut env: JNIEnv<'local>,
    _: JObject<'local>,
    request_json: JString<'local>,
) -> JString<'local> {
    let command = "autogen installed fdroid apply";
    let response = match jstring_to_string(&mut env, &request_json)
        .and_then(|raw| apply_fdroid_autogen(&raw))
    {
        Ok(data) => success_envelope(command, data),
        Err(error) => operation_error_envelope(command, error),
    };
    java_string_or_fallback(&mut env, response)
}

#[no_mangle]
pub extern "C" fn Java_net_xzos_upgradeall_getter_NativeLib_previewGithubAutogen<'local>(
    mut env: JNIEnv<'local>,
    _: JObject<'local>,
    context: JObject<'local>,
    request_json: JString<'local>,
) -> JString<'local> {
    let command = "autogen github preview";
    let response = match init_android_integrations(&mut env, &context)
        .map_err(BridgeOperationError::Initialize)
        .and_then(|()| jstring_to_string(&mut env, &request_json))
        .and_then(|raw| preview_github_autogen(&raw))
    {
        Ok(data) => success_envelope(command, data),
        Err(error) => operation_error_envelope(command, error),
    };
    java_string_or_fallback(&mut env, response)
}

#[no_mangle]
pub extern "C" fn Java_net_xzos_upgradeall_getter_NativeLib_applyGithubAutogen<'local>(
    mut env: JNIEnv<'local>,
    _: JObject<'local>,
    request_json: JString<'local>,
) -> JString<'local> {
    let command = "autogen github apply";
    let response = match jstring_to_string(&mut env, &request_json)
        .and_then(|raw| apply_github_autogen(&raw))
    {
        Ok(data) => success_envelope(command, data),
        Err(error) => operation_error_envelope(command, error),
    };
    java_string_or_fallback(&mut env, response)
}

#[no_mangle]
pub extern "C" fn Java_net_xzos_upgradeall_getter_NativeLib_previewFdroidAutogen<'local>(
    mut env: JNIEnv<'local>,
    _: JObject<'local>,
    request_json: JString<'local>,
) -> JString<'local> {
    let command = "autogen fdroid preview";
    let response = match jstring_to_string(&mut env, &request_json)
        .and_then(|raw| preview_fdroid_autogen(&raw))
    {
        Ok(data) => success_envelope(command, data),
        Err(error) => operation_error_envelope(command, error),
    };
    java_string_or_fallback(&mut env, response)
}

#[no_mangle]
pub extern "C" fn Java_net_xzos_upgradeall_getter_NativeLib_applyFdroidAutogen<'local>(
    mut env: JNIEnv<'local>,
    _: JObject<'local>,
    request_json: JString<'local>,
) -> JString<'local> {
    let command = "autogen fdroid apply";
    let response = match jstring_to_string(&mut env, &request_json)
        .and_then(|raw| apply_fdroid_autogen(&raw))
    {
        Ok(data) => success_envelope(command, data),
        Err(error) => operation_error_envelope(command, error),
    };
    java_string_or_fallback(&mut env, response)
}

#[no_mangle]
pub extern "C" fn Java_net_xzos_upgradeall_getter_NativeLib_refreshDefaultFdroidCatalogCache<
    'local,
>(
    mut env: JNIEnv<'local>,
    _: JObject<'local>,
    request_json: JString<'local>,
) -> JString<'local> {
    let command = "fdroid catalog refresh";
    let response = match jstring_to_string(&mut env, &request_json)
        .and_then(|raw| refresh_default_fdroid_catalog_cache(&raw))
    {
        Ok(data) => success_envelope(command, data),
        Err(error) => operation_error_envelope(command, error),
    };
    java_string_or_fallback(&mut env, response)
}

#[no_mangle]
pub extern "C" fn Java_net_xzos_upgradeall_getter_NativeLib_runtimeOperation<'local>(
    mut env: JNIEnv<'local>,
    _: JObject<'local>,
    request_json: JString<'local>,
) -> JString<'local> {
    let command = "runtime operation";
    let response = match jstring_to_string(&mut env, &request_json).and_then(runtime_operation) {
        Ok(data) => success_envelope(command, data),
        Err(error) => operation_error_envelope(command, error),
    };
    java_string_or_fallback(&mut env, response)
}

#[no_mangle]
pub extern "C" fn Java_net_xzos_upgradeall_getter_NativeLib_drainRuntimeNotifications<'local>(
    mut env: JNIEnv<'local>,
    _: JObject<'local>,
) -> JString<'local> {
    let command = "runtime notifications drain";
    let response = match drain_runtime_notifications() {
        Ok(data) => success_envelope(command, data),
        Err(error) => operation_error_envelope(command, error),
    };
    java_string_or_fallback(&mut env, response)
}

#[no_mangle]
pub extern "C" fn Java_net_xzos_upgradeall_getter_NativeLib_importLegacyRoomDatabase<'local>(
    mut env: JNIEnv<'local>,
    _: JObject<'local>,
    request_json: JString<'local>,
) -> JString<'local> {
    let command = "legacy import-room-db";
    let response = match jstring_to_string(&mut env, &request_json)
        .and_then(|raw| import_legacy_room_database(&raw))
    {
        Ok(data) => success_envelope(command, data),
        Err(error) => operation_error_envelope(command, error),
    };
    java_string_or_fallback(&mut env, response)
}

#[no_mangle]
pub extern "C" fn Java_net_xzos_upgradeall_getter_NativeLib_legacyReportList<'local>(
    mut env: JNIEnv<'local>,
    _: JObject<'local>,
    request_json: JString<'local>,
) -> JString<'local> {
    let command = "legacy report-list";
    let response =
        match jstring_to_string(&mut env, &request_json).and_then(|raw| legacy_report_list(&raw)) {
            Ok(data) => success_envelope(command, data),
            Err(error) => operation_error_envelope(command, error),
        };
    java_string_or_fallback(&mut env, response)
}

#[no_mangle]
pub extern "C" fn Java_net_xzos_upgradeall_getter_NativeLib_startup<'local>(
    mut env: JNIEnv<'local>,
    _: JObject<'local>,
    context: JObject<'local>,
    request_json: JString<'local>,
) -> JString<'local> {
    let command = "startup";
    let response = match init_android_integrations(&mut env, &context)
        .map_err(BridgeOperationError::Initialize)
        .and_then(|()| jstring_to_string(&mut env, &request_json))
        .and_then(startup_operation)
    {
        Ok(data) => success_envelope(command, data),
        Err(error) => operation_error_envelope(command, error),
    };
    java_string_or_fallback(&mut env, response)
}

#[no_mangle]
pub extern "C" fn Java_net_xzos_upgradeall_getter_NativeLib_readOperation<'local>(
    mut env: JNIEnv<'local>,
    _: JObject<'local>,
    request_json: JString<'local>,
) -> JString<'local> {
    let command = "read operation";
    let response = match jstring_to_string(&mut env, &request_json).and_then(read_operation) {
        Ok(data) => success_envelope(command, data),
        Err(error) => operation_error_envelope(command, error),
    };
    java_string_or_fallback(&mut env, response)
}

fn preview_installed_autogen(
    env: &mut JNIEnv<'_>,
    context: &JObject<'_>,
    request_json: &str,
) -> Result<Value, BridgeOperationError> {
    init_android_integrations(env, context).map_err(BridgeOperationError::Initialize)?;
    let request: PreviewInstalledAutogenRequest = serde_json::from_str(request_json)
        .map_err(|source| BridgeOperationError::InvalidRequest(source.to_string()))?;
    let db = open_main_db(&request.data_dir)?;
    let scan = scan_installed_inventory(request.scan_options)?;
    let inventory: getter::core::autogen::InstalledInventory =
        serde_json::to_value(&scan.inventory)
            .and_then(serde_json::from_value)
            .map_err(|source| BridgeOperationError::PlatformMalformed(source.to_string()))?;
    let plan = autogen::build_installed_autogen_plan(&request.data_dir, &db, &inventory)?;
    let mut preview = autogen::installed_preview_json(&request.data_dir, &plan)?;
    if let Some(object) = preview.as_object_mut() {
        object.insert(
            "scan".to_owned(),
            json!({
                "stats": scan.stats,
                "diagnostics": scan.diagnostics,
            }),
        );
    }
    Ok(preview)
}

fn preview_installed_fdroid_autogen(
    env: &mut JNIEnv<'_>,
    context: &JObject<'_>,
    request_json: &str,
) -> Result<Value, BridgeOperationError> {
    init_android_integrations(env, context).map_err(BridgeOperationError::Initialize)?;
    let request: PreviewInstalledAutogenRequest = serde_json::from_str(request_json)
        .map_err(|source| BridgeOperationError::InvalidRequest(source.to_string()))?;
    let scan = scan_installed_inventory(request.scan_options)?;
    preview_installed_fdroid_autogen_from_scan(&request.data_dir, scan)
}

fn preview_installed_fdroid_autogen_from_scan(
    data_dir: &Path,
    scan: upgradeall_platform_adapter::InstalledInventoryScanResult,
) -> Result<Value, BridgeOperationError> {
    let db = open_main_db(data_dir)?;
    let cache_db = open_cache_db(data_dir)?;
    fdroid_catalog::read_or_refresh_fdroid_catalog(
        &cache_db,
        FdroidEndpointConfig::default(),
        ProviderCacheMode::UseCached,
        || Err("F-Droid catalog cache is empty; refresh provider cache before installed F-Droid autogen preview".to_owned()),
    )
    .map_err(|source| BridgeOperationError::Autogen(source.to_string()))?;
    let inventory: getter::core::autogen::InstalledInventory =
        serde_json::to_value(&scan.inventory)
            .and_then(serde_json::from_value)
            .map_err(|source| BridgeOperationError::PlatformMalformed(source.to_string()))?;
    let payload = json!({ "installed_inventory": inventory });
    let mut preview = fdroid_autogen::preview_fdroid_packages_json(
        data_dir,
        &db,
        &cache_db,
        &payload.to_string(),
    )?;
    if let Some(object) = preview.as_object_mut() {
        object.insert(
            "scan".to_owned(),
            json!({
                "stats": scan.stats,
                "diagnostics": scan.diagnostics,
            }),
        );
    }
    Ok(preview)
}

fn apply_installed_autogen(request_json: &str) -> Result<Value, BridgeOperationError> {
    let request: ApplyInstalledAutogenRequest = serde_json::from_str(request_json)
        .map_err(|source| BridgeOperationError::InvalidRequest(source.to_string()))?;
    let db = open_main_db(&request.data_dir)?;
    let preview = autogen::unwrap_preview_payload(request.preview, "installed.preview")?;
    let acceptance = request.acceptance.into_autogen_acceptance()?;
    Ok(autogen::apply_installed_preview(
        &request.data_dir,
        &db,
        &preview,
        &acceptance,
    )?)
}

fn preview_fdroid_autogen(request_json: &str) -> Result<Value, BridgeOperationError> {
    let request: PreviewFdroidAutogenRequest = serde_json::from_str(request_json)
        .map_err(|source| BridgeOperationError::InvalidRequest(source.to_string()))?;
    let db = open_main_db(&request.data_dir)?;
    let cache_db = open_cache_db(&request.data_dir)?;
    let payload = if request.payload.is_null() {
        "{}".to_owned()
    } else {
        request.payload.to_string()
    };
    Ok(fdroid_autogen::preview_fdroid_packages_json(
        &request.data_dir,
        &db,
        &cache_db,
        &payload,
    )?)
}

fn apply_fdroid_autogen(request_json: &str) -> Result<Value, BridgeOperationError> {
    let request: ApplyFdroidAutogenRequest = serde_json::from_str(request_json)
        .map_err(|source| BridgeOperationError::InvalidRequest(source.to_string()))?;
    let db = open_main_db(&request.data_dir)?;
    let preview = autogen::unwrap_preview_payload(request.preview, "fdroid.autogen.preview")?;
    let acceptance = request.acceptance.into_autogen_acceptance()?;
    Ok(fdroid_autogen::apply_fdroid_preview_json(
        &request.data_dir,
        &db,
        &preview,
        &acceptance,
    )?)
}

fn preview_github_autogen(request_json: &str) -> Result<Value, BridgeOperationError> {
    let request: PreviewGithubAutogenRequest = serde_json::from_str(request_json)
        .map_err(|source| BridgeOperationError::InvalidRequest(source.to_string()))?;
    let db = open_main_db(&request.data_dir)?;
    let cache_db = open_cache_db(&request.data_dir)?;
    let payload = json!({
        "owner": request.owner,
        "repo": request.repo,
        "android_package": request.android_package,
        "display_name": request.display_name,
    });
    Ok(github_autogen::preview_github_android_package_json(
        &request.data_dir,
        &db,
        &cache_db,
        &payload.to_string(),
    )?)
}

fn apply_github_autogen(request_json: &str) -> Result<Value, BridgeOperationError> {
    let request: ApplyGithubAutogenRequest = serde_json::from_str(request_json)
        .map_err(|source| BridgeOperationError::InvalidRequest(source.to_string()))?;
    let db = open_main_db(&request.data_dir)?;
    let preview = autogen::unwrap_preview_payload(request.preview, "github.autogen.preview")?;
    let acceptance = request.acceptance.into_autogen_acceptance()?;
    Ok(github_autogen::apply_github_preview_json(
        &request.data_dir,
        &db,
        &preview,
        &acceptance,
    )?)
}

fn refresh_default_fdroid_catalog_cache(request_json: &str) -> Result<Value, BridgeOperationError> {
    let request: RefreshDefaultFdroidCatalogCacheRequest = serde_json::from_str(request_json)
        .map_err(|source| BridgeOperationError::InvalidRequest(source.to_string()))?;
    let cache_db = open_cache_db(&request.data_dir)?;
    let catalog = fdroid_catalog::read_or_refresh_fdroid_catalog(
        &cache_db,
        FdroidEndpointConfig::default(),
        ProviderCacheMode::ForceRefresh,
        || Ok(default_fdroid_catalog_xml().to_owned()),
    )
    .map_err(|source| BridgeOperationError::ProviderCatalog(source.to_string()))?;
    let release_count: usize = catalog
        .catalog
        .apps
        .iter()
        .map(|app| app.packages.len())
        .sum();

    Ok(json!({
        "operation": "fdroid.catalog.refresh",
        "provider": "fdroid",
        "endpoint_id": catalog.endpoint.endpoint_id,
        "endpoint_url": catalog.endpoint.endpoint_url,
        "cache_key": catalog.cache_key,
        "source": provider_cache_source_json(catalog.source),
        "app_count": catalog.catalog.apps.len(),
        "release_count": release_count,
        "source_response_sha512": catalog.source_response_sha512,
        "provenance_schema_version": catalog.provenance_schema_version,
        "diagnostics": catalog.diagnostics.iter().map(|diagnostic| {
            json!({
                "code": diagnostic.code,
                "message": diagnostic.message,
                "cache_key": diagnostic.cache_key,
                "provider": diagnostic.provider,
                "stale_fetched_at_unix": diagnostic.stale_fetched_at_unix,
            })
        }).collect::<Vec<_>>(),
    }))
}

fn provider_cache_source_json(source: ProviderCacheSource) -> &'static str {
    match source {
        ProviderCacheSource::Cache => "cache",
        ProviderCacheSource::Refreshed => "refreshed",
        ProviderCacheSource::Stale => "stale",
    }
}

fn default_fdroid_catalog_xml() -> &'static str {
    include_str!("../../getter/tests/files/web/f-droid.xml")
}

fn import_legacy_room_database(request_json: &str) -> Result<Value, BridgeOperationError> {
    let request: ImportLegacyRoomDatabaseRequest = serde_json::from_str(request_json)
        .map_err(|source| BridgeOperationError::InvalidRequest(source.to_string()))?;
    legacy_room::import_room_db_json(&request.data_dir, &request.database_path)
        .map_err(BridgeOperationError::from)
}

fn legacy_report_list(request_json: &str) -> Result<Value, BridgeOperationError> {
    let request: LegacyReportListRequest = serde_json::from_str(request_json)
        .map_err(|source| BridgeOperationError::InvalidRequest(source.to_string()))?;
    legacy_room::report_list_json(&request.data_dir).map_err(BridgeOperationError::from)
}

fn init_getter_runtime() -> &'static Mutex<getter::core::runtime::GetterRuntime> {
    GETTER_RUNTIME.get_or_init(|| {
        let mut runtime = getter::core::runtime::GetterRuntime::new();
        runtime.set_notification_sink(|notification| {
            enqueue_runtime_notification(notification);
        });
        Mutex::new(runtime)
    })
}

fn runtime_notification_queue() -> &'static Mutex<VecDeque<Value>> {
    RUNTIME_NOTIFICATIONS.get_or_init(|| Mutex::new(VecDeque::new()))
}

fn enqueue_runtime_notification(notification: getter::core::runtime::RuntimeNotification) {
    let Ok(value) = serde_json::to_value(notification) else {
        return;
    };
    let Ok(mut queue) = runtime_notification_queue().lock() else {
        return;
    };
    if queue.len() >= MAX_RUNTIME_NOTIFICATION_QUEUE {
        queue.pop_front();
    }
    queue.push_back(value);
}

fn drain_runtime_notifications() -> Result<Value, BridgeOperationError> {
    let mut queue = runtime_notification_queue()
        .lock()
        .map_err(|_| BridgeOperationError::RuntimeNotificationQueuePoisoned)?;
    let notifications: Vec<Value> = queue.drain(..).collect();
    Ok(json!({ "notifications": notifications }))
}

fn startup_operation(request_json: String) -> Result<Value, BridgeOperationError> {
    startup_operation_with_scanner(&request_json, scan_installed_inventory)
}

fn startup_operation_with_scanner<F>(
    request_json: &str,
    scanner: F,
) -> Result<Value, BridgeOperationError>
where
    F: FnOnce(
        InstalledInventoryScanOptions,
    ) -> Result<
        upgradeall_platform_adapter::InstalledInventoryScanResult,
        BridgeOperationError,
    >,
{
    let request: StartupRequest = serde_json::from_str(request_json)
        .map_err(|source| BridgeOperationError::InvalidRequest(source.to_string()))?;
    let scan = scanner(request.scan_options)?;
    let inventory = serde_json::to_value(&scan.inventory)
        .and_then(serde_json::from_value)
        .map_err(|source| BridgeOperationError::PlatformMalformed(source.to_string()))?;
    let mut snapshot = startup_operations::startup(&request.data_dir, inventory)?;
    snapshot
        .diagnostics
        .extend(scan.diagnostics.into_iter().map(|diagnostic| {
            startup_operations::StartupDiagnostic {
                code: diagnostic.code,
                message: diagnostic.message,
            }
        }));
    let mut value = serde_json::to_value(snapshot)
        .map_err(|source| BridgeOperationError::InvalidRequest(source.to_string()))?;
    value["platform"] = json!({
        "inventory_scan": {
            "stats": scan.stats,
        }
    });
    Ok(value)
}

fn read_operation(request_json: String) -> Result<Value, BridgeOperationError> {
    let request: ReadOperationRequest = serde_json::from_str(&request_json)
        .map_err(|source| BridgeOperationError::InvalidRequest(source.to_string()))?;
    let payload = if request.payload.is_null() {
        "{}".to_owned()
    } else {
        request.payload.to_string()
    };
    match request.operation.as_str() {
        "repository_list" => read_model::repository_list_json(&request.data_dir),
        "tracked_package_list" => read_model::tracked_package_list_json(&request.data_dir),
        "package_eval" => read_model::package_eval_json(&request.data_dir, &payload),
        other => Err(ReadModelOperationError::InvalidRequest(format!(
            "unsupported read operation '{other}'"
        ))),
    }
    .map_err(BridgeOperationError::ReadModel)
}

fn runtime_operation(request_json: String) -> Result<Value, BridgeOperationError> {
    let runtime = init_getter_runtime();
    let mut runtime = runtime
        .lock()
        .map_err(|_| BridgeOperationError::RuntimePoisoned)?;
    runtime_operation_with_runtime(&mut runtime, &request_json)
}

fn runtime_operation_with_runtime(
    runtime: &mut getter::core::runtime::GetterRuntime,
    request_json: &str,
) -> Result<Value, BridgeOperationError> {
    let request: RuntimeOperationRequest = serde_json::from_str(request_json)
        .map_err(|source| BridgeOperationError::InvalidRequest(source.to_string()))?;
    let payload = if request.payload.is_null() {
        "{}".to_owned()
    } else {
        request.payload.to_string()
    };
    match request.operation.as_str() {
        "update_check_offline_issue_action" => {
            runtime_operations::issue_action_from_offline_update_check_json(runtime, &payload)
        }
        "update_check_package_issue_action" => {
            let data_dir = request.data_dir.as_ref().ok_or_else(|| {
                BridgeOperationError::InvalidRequest(
                    "data_dir is required for package update checks".to_owned(),
                )
            })?;
            let db = open_main_db(data_dir)?;
            runtime_operations::issue_action_from_registered_package_json(
                runtime, data_dir, &db, &payload,
            )
        }
        "task_submit" => {
            if let Some(data_dir) = request.data_dir.as_ref() {
                runtime_operations::submit_action_and_download_json(runtime, data_dir, &payload)
            } else {
                runtime_operations::submit_action_json(runtime, &payload)
            }
        }
        "task_get" => runtime_operations::task_get_json(runtime, &payload),
        "task_list" => runtime_operations::task_list_json(runtime, &payload),
        "task_start" => runtime_operations::task_start_json(runtime, &payload),
        "task_download_progress" => {
            runtime_operations::task_download_progress_json(runtime, &payload)
        }
        "task_complete_download" => {
            runtime_operations::task_complete_download_json(runtime, &payload)
        }
        "task_pause" => runtime_operations::task_pause_json(runtime, &payload),
        "task_resume" => runtime_operations::task_resume_json(runtime, &payload),
        "task_user_result" => runtime_operations::task_user_result_json(runtime, &payload),
        "task_cancel" => runtime_operations::task_cancel_json(runtime, &payload),
        "task_retry" => {
            if let Some(data_dir) = request.data_dir.as_ref() {
                runtime_operations::task_retry_download_json(runtime, data_dir, &payload)
            } else {
                runtime_operations::task_retry_json(runtime, &payload)
            }
        }
        "task_remove" => runtime_operations::task_remove_json(runtime, &payload),
        "task_clean" => runtime_operations::task_clean_json(runtime, &payload),
        other => Err(runtime_operations::RuntimeOperationError::InvalidRequest(
            format!("unsupported runtime operation '{other}'"),
        )),
    }
    .map_err(BridgeOperationError::Runtime)
}

impl ApplyAutogenAcceptance {
    fn into_autogen_acceptance(self) -> Result<AutogenAcceptance, BridgeOperationError> {
        match self.mode.as_deref().unwrap_or("all") {
            "all" => Ok(AutogenAcceptance::AcceptAll),
            "packages" => Ok(AutogenAcceptance::Accept(self.package_ids)),
            other => Err(BridgeOperationError::InvalidRequest(format!(
                "unsupported autogen acceptance mode '{other}'"
            ))),
        }
    }
}

fn open_main_db(data_dir: &Path) -> Result<getter::storage::MainDb, BridgeOperationError> {
    std::fs::create_dir_all(data_dir)
        .map_err(|source| BridgeOperationError::Storage(source.to_string()))?;
    getter::storage::CacheDb::open(data_dir.join(CACHE_DB_FILE))?;
    Ok(getter::storage::MainDb::open(data_dir.join(MAIN_DB_FILE))?)
}

fn open_cache_db(data_dir: &Path) -> Result<getter::storage::CacheDb, BridgeOperationError> {
    std::fs::create_dir_all(data_dir)
        .map_err(|source| BridgeOperationError::Storage(source.to_string()))?;
    Ok(getter::storage::CacheDb::open(
        data_dir.join(CACHE_DB_FILE),
    )?)
}

fn scan_installed_inventory(
    options: InstalledInventoryScanOptions,
) -> Result<upgradeall_platform_adapter::InstalledInventoryScanResult, BridgeOperationError> {
    #[cfg(target_os = "android")]
    {
        upgradeall_platform_adapter::android::AndroidPlatformAdapter
            .scan_installed_inventory(options)
            .map_err(BridgeOperationError::Platform)
    }
    #[cfg(not(target_os = "android"))]
    {
        let _ = options;
        Err(BridgeOperationError::Platform(
            upgradeall_platform_adapter::PlatformAdapterError::Unsupported {
                capability: "installed_inventory.android",
            },
        ))
    }
}

fn init_android_integrations(env: &mut JNIEnv<'_>, context: &JObject<'_>) -> Result<(), String> {
    // Initialize Android-hosted Rust platform integrations for future use.
    // https://github.com/rustls/rustls-platform-verifier/tree/3edb4d278215a8603020351b8b519d907a26041f?tab=readme-ov-file#crate-initialization
    #[cfg(target_os = "android")]
    {
        let rustls_context = env
            .new_local_ref(context)
            .map_err(|e| format!("Error creating rustls context ref: {e}"))?;
        rustls_platform_verifier::android::init_hosted(env, rustls_context)
            .map_err(|e| format!("Error initializing certificate verifier: {e}"))?;

        let platform_context = env
            .new_local_ref(context)
            .map_err(|e| format!("Error creating platform adapter context ref: {e}"))?;
        upgradeall_platform_adapter::android::init_with_env(env, platform_context)
            .map_err(|e| format!("Error initializing platform adapter: {e}"))?;
    }
    #[cfg(not(target_os = "android"))]
    {
        let _ = env;
        let _ = context;
    }
    Ok(())
}

fn jstring_to_string(
    env: &mut JNIEnv<'_>,
    value: &JString<'_>,
) -> Result<String, BridgeOperationError> {
    env.get_string(value)
        .map(|value| value.into())
        .map_err(|source| BridgeOperationError::Jni(source.to_string()))
}

fn java_string_or_fallback<'local>(
    env: &mut JNIEnv<'local>,
    value: impl AsRef<str>,
) -> JString<'local> {
    env.new_string(value.as_ref()).unwrap_or_else(|_| {
        env.new_string("JNI string allocation failed")
            .expect("fallback string")
    })
}

fn success_envelope(command: &str, data: Value) -> String {
    json!({
        "ok": true,
        "command": command,
        "data": data,
        "warnings": [],
    })
    .to_string()
}

fn operation_error_envelope(command: &str, error: BridgeOperationError) -> String {
    let (code, message, detail) = error.parts();
    error_envelope(command, code, message, detail)
}

fn error_envelope(command: &str, code: &str, message: &str, detail: Option<String>) -> String {
    json!({
        "ok": false,
        "command": command,
        "error": {
            "code": code,
            "message": message,
            "detail": detail,
        },
    })
    .to_string()
}

#[derive(Debug, thiserror::Error)]
enum BridgeOperationError {
    #[error("invalid bridge request: {0}")]
    InvalidRequest(String),
    #[error("JNI error: {0}")]
    Jni(String),
    #[error("bridge initialization failed: {0}")]
    Initialize(String),
    #[error("platform error: {0}")]
    Platform(#[from] upgradeall_platform_adapter::PlatformAdapterError),
    #[error("platform inventory response is malformed: {0}")]
    PlatformMalformed(String),
    #[error("storage error: {0}")]
    Storage(String),
    #[error("repository error: {0}")]
    Repository(String),
    #[error("autogen error: {0}")]
    Autogen(String),
    #[error("F-Droid catalog provider error: {0}")]
    ProviderCatalog(String),
    #[error("migration error: {0}")]
    Migration(#[from] LegacyRoomOperationError),
    #[error("read model error: {0}")]
    ReadModel(#[from] ReadModelOperationError),
    #[error("startup error: {0}")]
    Startup(#[from] startup_operations::StartupError),
    #[error("runtime error: {0}")]
    Runtime(#[from] runtime_operations::RuntimeOperationError),
    #[error("runtime lock is poisoned")]
    RuntimePoisoned,
    #[error("runtime notification queue is poisoned")]
    RuntimeNotificationQueuePoisoned,
}

impl BridgeOperationError {
    fn parts(self) -> (&'static str, &'static str, Option<String>) {
        match self {
            Self::InvalidRequest(detail) => (
                "bridge.invalid_request",
                "Getter native bridge request is invalid",
                Some(detail),
            ),
            Self::Jni(detail) => (
                "bridge.jni_error",
                "Getter native bridge JNI operation failed",
                Some(detail),
            ),
            Self::Initialize(detail) => (
                "bridge.initialize_error",
                "Getter native bridge initialization failed",
                Some(detail),
            ),
            Self::Platform(upgradeall_platform_adapter::PlatformAdapterError::Unsupported {
                capability,
            }) => (
                "platform.unsupported",
                "Android platform capability is unsupported",
                Some(capability.to_owned()),
            ),
            Self::Platform(upgradeall_platform_adapter::PlatformAdapterError::NotInitialized) => (
                "platform.not_initialized",
                "Android platform adapter is not initialized",
                None,
            ),
            Self::Platform(upgradeall_platform_adapter::PlatformAdapterError::Jni(detail)) => (
                "platform.jni_error",
                "Android platform adapter JNI operation failed",
                Some(detail),
            ),
            Self::Platform(
                upgradeall_platform_adapter::PlatformAdapterError::MalformedResponse(detail),
            ) => (
                "platform.malformed_response",
                "Android platform adapter response is malformed",
                Some(detail),
            ),
            Self::PlatformMalformed(detail) => (
                "platform.malformed_response",
                "Android platform inventory response is malformed",
                Some(detail),
            ),
            Self::Storage(detail) => (
                "storage.error",
                "Getter storage operation failed",
                Some(detail),
            ),
            Self::Repository(detail) => (
                "repository.error",
                "Getter repository operation failed",
                Some(detail),
            ),
            Self::Autogen(detail) => (
                "autogen.error",
                "Getter autogen operation failed",
                Some(detail),
            ),
            Self::ProviderCatalog(detail) => (
                "provider.fdroid_catalog.error",
                "F-Droid catalog cache refresh failed",
                Some(detail),
            ),
            Self::Migration(error) => (
                error.code(),
                error.message(),
                error
                    .detail()
                    .or_else(|| error.report_path().map(|path| path.display().to_string())),
            ),
            Self::ReadModel(error) => (error.code(), error.message(), error.detail()),
            Self::Startup(error) => (
                "startup.failed",
                "Getter startup failed",
                Some(error.to_string()),
            ),
            Self::Runtime(error) => (error.code(), error.message(), error.detail()),
            Self::RuntimePoisoned => ("runtime.poisoned", "Getter runtime lock is poisoned", None),
            Self::RuntimeNotificationQueuePoisoned => (
                "runtime.notification_queue_poisoned",
                "Getter runtime notification queue is poisoned",
                None,
            ),
        }
    }
}

impl From<getter::storage::StorageError> for BridgeOperationError {
    fn from(value: getter::storage::StorageError) -> Self {
        Self::Storage(value.to_string())
    }
}

impl From<AutogenOperationError> for BridgeOperationError {
    fn from(value: AutogenOperationError) -> Self {
        match value {
            AutogenOperationError::Storage(source) => Self::Storage(source.to_string()),
            AutogenOperationError::Repository(detail) => Self::Repository(detail),
            AutogenOperationError::MissingGeneratedRepository { .. } => {
                Self::Autogen(value.to_string())
            }
            AutogenOperationError::Autogen(detail) => Self::Autogen(detail),
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use getter::core::{
        autogen::{FDROID_AUTOGEN_GENERATOR, GITHUB_AUTOGEN_GENERATOR},
        repository::{RepositoryMetadata, RepositoryPackageDirectoryLayout, REPO_API_VERSION_V1},
        runtime::{PackageVersionLuaObject, SealedActionPlan},
        RepositoryPriority, UpdateAction,
    };
    use std::fs;
    use std::io::{Read, Write};
    use std::net::TcpListener;
    use std::thread;

    const GITHUB_RELEASES_FIXTURE: &str =
        include_str!("../../getter/tests/files/web/github_api_release.json");

    #[test]
    fn packages_acceptance_defaults_to_all() {
        let acceptance = ApplyAutogenAcceptance::default()
            .into_autogen_acceptance()
            .expect("acceptance");

        assert!(matches!(acceptance, AutogenAcceptance::AcceptAll));
    }

    #[test]
    fn packages_acceptance_preserves_getter_package_ids() {
        let acceptance = ApplyAutogenAcceptance {
            mode: Some("packages".to_owned()),
            package_ids: vec!["android/org.fdroid.fdroid".parse().expect("package id")],
        }
        .into_autogen_acceptance()
        .expect("acceptance");

        match acceptance {
            AutogenAcceptance::Accept(ids) => {
                assert_eq!(ids[0].to_string(), "android/org.fdroid.fdroid")
            }
            AutogenAcceptance::AcceptAll => panic!("expected explicit package acceptance"),
        }
    }

    #[test]
    fn fdroid_autogen_bridge_preview_and_apply_write_package_directories() {
        let temp = tempfile::tempdir().unwrap();
        let data_dir = temp.path().join("data");
        let request = json!({
            "data_dir": data_dir,
            "payload": {
                "index_xml": fdroid_fixture(),
                "package_names": ["org.fdroid.fdroid"]
            }
        });

        let preview = preview_fdroid_autogen(&request.to_string()).expect("F-Droid preview");

        assert_eq!(preview["operation"], "fdroid.autogen.preview");
        assert_eq!(
            preview["candidates"][0]["package_id"],
            "android/f-droid/app/org.fdroid.fdroid"
        );

        let apply = apply_fdroid_autogen(
            &json!({
                "data_dir": data_dir,
                "preview": preview,
                "acceptance": { "mode": "packages", "package_ids": ["android/f-droid/app/org.fdroid.fdroid"] }
            })
            .to_string(),
        )
        .expect("F-Droid apply");

        assert_eq!(apply["applied_count"], 1);
        assert_eq!(
            apply["applied"][0]["package_id"],
            "android/f-droid/app/org.fdroid.fdroid"
        );
        let repo_root = temp.path().join("data/repo/autogen");
        let package_dir = repo_root.join("android/f-droid/app/org.fdroid.fdroid");
        assert!(package_dir.join("metadata.jsonc").is_file());
        assert!(package_dir.join("Manifest").is_file());
        assert!(package_dir.join("9999.lua").is_file());
        assert!(package_dir.join(".autogen.jsonc").is_file());
        let record: Value = serde_json::from_str(
            &std::fs::read_to_string(package_dir.join(".autogen.jsonc")).unwrap(),
        )
        .unwrap();
        assert_eq!(record["generator"], FDROID_AUTOGEN_GENERATOR);
        assert_eq!(record["input"]["package_name"], "org.fdroid.fdroid");
        let layout = RepositoryPackageDirectoryLayout::load(&repo_root).unwrap();
        assert!(layout
            .package(&"android/f-droid/app/org.fdroid.fdroid".parse().unwrap())
            .is_some());
    }

    #[test]
    fn github_autogen_bridge_preview_and_apply_write_package_directories_from_cache() {
        let temp = tempfile::tempdir().unwrap();
        let data_dir = temp.path().join("data");
        let cache_db = open_cache_db(&data_dir).unwrap();
        getter::operations::github_releases::read_or_refresh_github_releases(
            &cache_db,
            getter::operations::github_releases::GithubReleaseConfig {
                api_base_url: getter::operations::github_releases::DEFAULT_GITHUB_API_BASE_URL
                    .to_owned(),
                owner: "DUpdateSystem".to_owned(),
                repo: "UpgradeAll".to_owned(),
            },
            ProviderCacheMode::UseCached,
            || Ok(GITHUB_RELEASES_FIXTURE.to_owned()),
        )
        .unwrap();

        let preview = preview_github_autogen(
            &json!({
                "data_dir": data_dir,
                "owner": "DUpdateSystem",
                "repo": "UpgradeAll",
                "android_package": "net.xzos.upgradeall",
                "display_name": "UpgradeAll"
            })
            .to_string(),
        )
        .expect("GitHub preview");

        assert_eq!(preview["operation"], "github.autogen.preview");
        assert_eq!(preview["source"], "cache");
        assert_eq!(
            preview["candidates"][0]["package_id"],
            "android/github/DUpdateSystem/UpgradeAll/net.xzos.upgradeall"
        );
        assert!(!preview.to_string().contains("releases_json"));

        let apply = apply_github_autogen(
            &json!({
                "data_dir": data_dir,
                "preview": preview,
                "acceptance": {
                    "mode": "packages",
                    "package_ids": ["android/github/DUpdateSystem/UpgradeAll/net.xzos.upgradeall"]
                }
            })
            .to_string(),
        )
        .expect("GitHub apply");

        assert_eq!(apply["applied_count"], 1);
        let repo_root = temp.path().join("data/repo/autogen");
        let package_dir =
            repo_root.join("android/github/DUpdateSystem/UpgradeAll/net.xzos.upgradeall");
        assert!(package_dir.join("metadata.jsonc").is_file());
        assert!(package_dir.join("Manifest").is_file());
        assert!(package_dir.join("9999.lua").is_file());
        assert!(package_dir.join(".autogen.jsonc").is_file());
        let record: Value = serde_json::from_str(
            &std::fs::read_to_string(package_dir.join(".autogen.jsonc")).unwrap(),
        )
        .unwrap();
        assert_eq!(record["generator"], GITHUB_AUTOGEN_GENERATOR);
    }

    #[test]
    fn github_autogen_preview_rejects_product_provider_controls() {
        let temp = tempfile::tempdir().unwrap();
        let data_dir = temp.path().join("data");

        let error = preview_github_autogen(
            &json!({
                "data_dir": data_dir,
                "owner": "DUpdateSystem",
                "repo": "UpgradeAll",
                "android_package": "net.xzos.upgradeall",
                "releases_json": "[]"
            })
            .to_string(),
        )
        .unwrap_err();

        let (code, _, detail) = error.parts();
        assert_eq!(code, "bridge.invalid_request");
        let detail = detail.unwrap();
        assert!(detail.contains("unknown field"));
        assert!(detail.contains("releases_json"));
    }

    #[test]
    fn installed_fdroid_preview_reuses_platform_inventory_as_provider_request() {
        let temp = tempfile::tempdir().unwrap();
        let data_dir = temp.path().join("data");
        let cache_db = open_cache_db(&data_dir).unwrap();
        fdroid_catalog::read_or_refresh_fdroid_catalog(
            &cache_db,
            FdroidEndpointConfig::default(),
            ProviderCacheMode::UseCached,
            || Ok(fdroid_fixture().to_owned()),
        )
        .unwrap();
        let scan = upgradeall_platform_adapter::InstalledInventoryScanResult {
            inventory: upgradeall_platform_adapter::InstalledInventory::new(vec![
                upgradeall_platform_adapter::InstalledInventoryItem::AndroidPackage {
                    package_name: "org.fdroid.fdroid".to_owned(),
                    label: Some("F-Droid".to_owned()),
                    version_name: Some("1.20.0".to_owned()),
                    version_code: Some(1_020_000),
                },
            ]),
            stats: upgradeall_platform_adapter::InstalledInventoryScanStats {
                total_seen: 2,
                returned: 1,
                filtered_system: 1,
                filtered_self: 0,
            },
            diagnostics: vec![upgradeall_platform_adapter::PlatformDiagnostic {
                code: "platform.note".to_owned(),
                message: "scan diagnostic".to_owned(),
                detail: None,
            }],
        };

        let preview = preview_installed_fdroid_autogen_from_scan(&data_dir, scan).unwrap();

        assert_eq!(preview["operation"], "fdroid.autogen.preview");
        assert_eq!(preview["source"], "cache");
        assert_eq!(preview["scan"]["stats"]["returned"], 1);
        assert_eq!(preview["scan"]["diagnostics"][0]["code"], "platform.note");
        assert_eq!(
            preview["candidates"][0]["package_id"],
            "android/f-droid/app/org.fdroid.fdroid"
        );
    }

    #[test]
    fn installed_fdroid_preview_requires_cached_catalog() {
        let temp = tempfile::tempdir().unwrap();
        let data_dir = temp.path().join("data");
        let scan = upgradeall_platform_adapter::InstalledInventoryScanResult {
            inventory: upgradeall_platform_adapter::InstalledInventory::new(vec![
                upgradeall_platform_adapter::InstalledInventoryItem::AndroidPackage {
                    package_name: "org.fdroid.fdroid".to_owned(),
                    label: None,
                    version_name: None,
                    version_code: None,
                },
            ]),
            stats: upgradeall_platform_adapter::InstalledInventoryScanStats {
                total_seen: 1,
                returned: 1,
                filtered_system: 0,
                filtered_self: 0,
            },
            diagnostics: Vec::new(),
        };

        let error = preview_installed_fdroid_autogen_from_scan(&data_dir, scan).unwrap_err();

        let detail = error.to_string();
        assert!(detail.contains("F-Droid catalog cache is empty"));
        assert!(!detail.contains("index_xml"));
    }

    #[test]
    fn default_fdroid_catalog_refresh_populates_installed_preview_cache() {
        let temp = tempfile::tempdir().unwrap();
        let data_dir = temp.path().join("data");
        let refresh = refresh_default_fdroid_catalog_cache(
            &json!({
                "data_dir": data_dir,
            })
            .to_string(),
        )
        .expect("refresh default catalog");

        assert_eq!(refresh["operation"], "fdroid.catalog.refresh");
        assert_eq!(refresh["provider"], "fdroid");
        assert_eq!(refresh["endpoint_id"], "official");
        assert_eq!(refresh["source"], "refreshed");
        assert!(refresh["app_count"].as_u64().unwrap() > 0);
        assert!(refresh["release_count"].as_u64().unwrap() > 0);
        assert_eq!(
            refresh["source_response_sha512"].as_array().unwrap().len(),
            1
        );
        assert_eq!(
            refresh["provenance_schema_version"],
            getter::operations::provider_cache::PROVIDER_RESPONSE_PROVENANCE_SCHEMA_V1
        );
        assert!(!refresh.to_string().contains("index_xml"));

        let scan = upgradeall_platform_adapter::InstalledInventoryScanResult {
            inventory: upgradeall_platform_adapter::InstalledInventory::new(vec![
                upgradeall_platform_adapter::InstalledInventoryItem::AndroidPackage {
                    package_name: "org.fdroid.fdroid".to_owned(),
                    label: Some("F-Droid".to_owned()),
                    version_name: Some("1.20.0".to_owned()),
                    version_code: Some(1_020_000),
                },
            ]),
            stats: upgradeall_platform_adapter::InstalledInventoryScanStats {
                total_seen: 1,
                returned: 1,
                filtered_system: 0,
                filtered_self: 0,
            },
            diagnostics: Vec::new(),
        };
        let preview = preview_installed_fdroid_autogen_from_scan(&data_dir, scan)
            .expect("installed F-Droid preview after refresh");

        assert_eq!(preview["source"], "cache");
        assert_eq!(
            preview["candidates"][0]["package_id"],
            "android/f-droid/app/org.fdroid.fdroid"
        );
    }

    #[test]
    fn startup_scans_platform_inventory_and_surfaces_stable_scan_facts() {
        let temp = tempfile::tempdir().unwrap();
        let data_dir = temp.path().join("data");
        let repo_root = temp.path().join("repo");
        write_static_update_repo(&repo_root);
        let db = open_main_db(&data_dir).unwrap();
        let repository_id: getter::core::RepositoryId = "official".parse().unwrap();
        db.upsert_repository(
            &RepositoryMetadata {
                id: repository_id.clone(),
                name: "Official".to_owned(),
                priority: RepositoryPriority::DEFAULT,
                api_version: REPO_API_VERSION_V1.to_owned(),
            },
            Some(&repo_root),
            None,
        )
        .unwrap();
        db.upsert_tracked_package(&getter::storage::TrackedPackageUpsert {
            package_id: "android/org.fdroid.fdroid".parse().unwrap(),
            enabled: true,
            favorite: false,
            pin_version: None,
            repository_id: Some(repository_id),
            package_resolution: getter::storage::StoredPackageResolution::OfficialRepositoryPackage,
        })
        .unwrap();
        let mut scanned_options = None;

        let snapshot = startup_operation_with_scanner(
            &json!({
                "data_dir": data_dir,
                "scan_options": {"include_system_apps": true, "include_self": false}
            })
            .to_string(),
            |options| {
                scanned_options = Some(options.clone());
                Ok(upgradeall_platform_adapter::InstalledInventoryScanResult {
                    inventory: upgradeall_platform_adapter::InstalledInventory::new(vec![
                        upgradeall_platform_adapter::InstalledInventoryItem::AndroidPackage {
                            package_name: "org.fdroid.fdroid".to_owned(),
                            label: Some("F-Droid".to_owned()),
                            version_name: Some("2.4.0".to_owned()),
                            version_code: Some(24),
                        },
                    ]),
                    stats: upgradeall_platform_adapter::InstalledInventoryScanStats {
                        total_seen: 2,
                        returned: 1,
                        filtered_system: 1,
                        filtered_self: 0,
                    },
                    diagnostics: vec![upgradeall_platform_adapter::PlatformDiagnostic {
                        code: "platform.partial_inventory".to_owned(),
                        message: "One package could not be inspected".to_owned(),
                        detail: Some("platform-only detail".to_owned()),
                    }],
                })
            },
        )
        .unwrap();

        assert_eq!(scanned_options.unwrap().include_system_apps, true);
        assert_eq!(snapshot["apps"][0]["installed_version"], "2.4.0");
        assert_eq!(
            snapshot["platform"]["inventory_scan"]["stats"]["returned"],
            1
        );
        assert!(snapshot["diagnostics"]
            .as_array()
            .unwrap()
            .iter()
            .any(|item| {
                item["code"] == "platform.partial_inventory"
                    && item["message"] == "One package could not be inspected"
                    && item.get("detail").is_none()
            }));
    }

    #[test]
    fn startup_rejects_domain_and_transport_inputs_before_platform_scan() {
        let temp = tempfile::tempdir().unwrap();
        for forbidden in [
            (
                "inventory",
                json!({"format": "upgradeall-installed-inventory", "version": 1, "items": []}),
            ),
            ("provider", json!("github")),
            ("cache", json!(true)),
            ("endpoint", json!("https://example.invalid")),
            ("transport", json!("live")),
        ] {
            let mut request = serde_json::Map::from_iter([
                ("data_dir".to_owned(), json!(temp.path().join("data"))),
                ("scan_options".to_owned(), json!({})),
            ]);
            request.insert(forbidden.0.to_owned(), forbidden.1);

            let error = startup_operation(Value::Object(request).to_string()).unwrap_err();
            assert!(
                matches!(error, BridgeOperationError::InvalidRequest(_)),
                "{} must be rejected at the startup request boundary: {error}",
                forbidden.0,
            );
        }
    }

    #[test]
    fn default_fdroid_catalog_refresh_rejects_provider_controls() {
        let temp = tempfile::tempdir().unwrap();
        let data_dir = temp.path().join("data");
        let error = refresh_default_fdroid_catalog_cache(
            &json!({
                "data_dir": data_dir,
                "index_xml": "<fdroid />",
            })
            .to_string(),
        )
        .unwrap_err();

        let (code, _, detail) = error.parts();
        assert_eq!(code, "bridge.invalid_request");
        let detail = detail.unwrap();
        assert!(detail.contains("unknown field"));
        assert!(detail.contains("index_xml"));
    }

    #[test]
    fn read_operation_lists_repositories_and_evaluates_packages() {
        let temp = tempfile::tempdir().unwrap();
        let data_dir = temp.path().join("data");
        let repo_root = temp.path().join("repo");
        write_static_update_repo(&repo_root);
        let db = open_main_db(&data_dir).unwrap();
        db.upsert_repository(
            &RepositoryMetadata {
                id: "official".parse().unwrap(),
                name: "Official".to_owned(),
                priority: RepositoryPriority::new(0),
                api_version: REPO_API_VERSION_V1.to_owned(),
            },
            Some(&repo_root),
            None,
        )
        .unwrap();

        let repositories = read_operation(
            json!({
                "operation": "repository_list",
                "data_dir": data_dir,
            })
            .to_string(),
        )
        .expect("repository list");
        assert_eq!(repositories["repositories"][0]["id"], "official");

        let package = read_operation(
            json!({
                "operation": "package_eval",
                "data_dir": data_dir,
                "payload": { "package_id": "android/org.fdroid.fdroid" }
            })
            .to_string(),
        )
        .expect("package eval");
        assert_eq!(package["package"]["id"], "android/org.fdroid.fdroid");
        assert_eq!(package["package"]["repository"], "official");
    }

    #[test]
    fn runtime_dispatcher_issues_action_from_registered_package_update_check() {
        let temp = tempfile::tempdir().unwrap();
        let data_dir = temp.path().join("data");
        let repo_root = temp.path().join("repo");
        write_static_update_repo(&repo_root);
        let db = open_main_db(&data_dir).unwrap();
        db.upsert_repository(
            &RepositoryMetadata {
                id: "official".parse().unwrap(),
                name: "Official".to_owned(),
                priority: RepositoryPriority::new(0),
                api_version: REPO_API_VERSION_V1.to_owned(),
            },
            Some(&repo_root),
            None,
        )
        .unwrap();
        let mut runtime = getter::core::runtime::GetterRuntime::new();

        let issued = runtime_operation_with_runtime(
            &mut runtime,
            &json!({
                "operation": "update_check_package_issue_action",
                "data_dir": data_dir,
                "payload": {
                    "package_id": "android/org.fdroid.fdroid",
                    "installed_version": "1.0.0"
                }
            })
            .to_string(),
        )
        .expect("issue action");

        assert_eq!(issued["package"]["repository"], "official");
        assert_eq!(issued["update"]["status"], "update_available");
        assert!(issued["action"]["action_id"].as_str().is_some());
    }

    #[test]
    fn runtime_dispatcher_issues_action_from_offline_update_check() {
        let mut runtime = getter::core::runtime::GetterRuntime::new();

        let issued = runtime_operation_with_runtime(
            &mut runtime,
            &json!({
                "operation": "update_check_offline_issue_action",
                "payload": {
                    "fixture": {
                        "format": "getter-offline-update-check",
                        "version": 1,
                        "package_id": "android/org.fdroid.fdroid",
                        "installed_version": "1.0.0",
                        "candidates": [
                            {
                                "version": "1.2.0",
                                "artifacts": [
                                    {
                                        "name": "app.apk",
                                        "url": "https://example.invalid/app.apk",
                                        "file_name": "app.apk"
                                    }
                                ]
                            }
                        ]
                    }
                }
            })
            .to_string(),
        )
        .expect("issue action");

        assert_eq!(issued["update"]["status"], "update_available");
        let action_id = issued["action"]["action_id"].as_str().expect("action id");
        let submitted = runtime_operation_with_runtime(
            &mut runtime,
            &json!({
                "operation": "task_submit",
                "payload": { "action_id": action_id }
            })
            .to_string(),
        )
        .expect("submit issued action");
        assert_eq!(submitted["package_id"], "android/org.fdroid.fdroid");
    }

    #[test]
    fn runtime_dispatcher_submit_with_data_dir_downloads_bytes() {
        let temp = tempfile::tempdir().unwrap();
        let data_dir = temp.path().join("data");
        let (url, handle) = serve_one_download_response(b"bridge bytes");
        let mut runtime = getter::core::runtime::GetterRuntime::new();
        let action = runtime_operations::issue_action(
            &mut runtime,
            SealedActionPlan {
                package_id: "generic/example".parse().expect("package id"),
                actions: vec![UpdateAction::Download {
                    url,
                    file_name: "source.bin".to_owned(),
                }],
                lua_object: PackageVersionLuaObject {
                    object_id: "lua:generic/example".to_owned(),
                    dependency_digest: "sha256:test".to_owned(),
                },
            },
        );
        let action_id = action["action_id"].as_str().expect("action id");

        let completed = runtime_operation_with_runtime(
            &mut runtime,
            &json!({
                "operation": "task_submit",
                "data_dir": data_dir,
                "payload": { "action_id": action_id }
            })
            .to_string(),
        )
        .expect("submit and download");

        assert_eq!(completed["status"], "completed");
        assert_eq!(completed["downloaded_file"]["file_name"], "source.bin");
        assert_eq!(completed["downloaded_file"]["size_bytes"], 12);
        let local_path = completed["downloaded_file"]["local_path"].as_str().unwrap();
        assert_eq!(fs::read(local_path).unwrap(), b"bridge bytes");
        assert!(local_path.contains("downloads/task-1/source.bin"));
        assert!(handle
            .join()
            .unwrap()
            .starts_with("GET /source.bin HTTP/1.1"));
    }

    #[test]
    fn runtime_dispatcher_retry_with_data_dir_downloads_failed_task_again() {
        let temp = tempfile::tempdir().unwrap();
        let data_dir = temp.path().join("data");
        let (url, handle) = serve_failing_then_successful_download_response(b"retry bytes");
        let mut runtime = getter::core::runtime::GetterRuntime::new();
        let action = runtime_operations::issue_action(
            &mut runtime,
            SealedActionPlan {
                package_id: "generic/example".parse().expect("package id"),
                actions: vec![UpdateAction::Download {
                    url,
                    file_name: "retry.bin".to_owned(),
                }],
                lua_object: PackageVersionLuaObject {
                    object_id: "lua:generic/example".to_owned(),
                    dependency_digest: "sha256:test".to_owned(),
                },
            },
        );
        let action_id = action["action_id"].as_str().expect("action id");
        let failed = runtime_operation_with_runtime(
            &mut runtime,
            &json!({
                "operation": "task_submit",
                "data_dir": data_dir,
                "payload": { "action_id": action_id }
            })
            .to_string(),
        )
        .expect("initial failed download task");
        assert_eq!(failed["status"], "failed");

        let task_id = failed["task_id"].as_str().expect("task id");
        let retried = runtime_operation_with_runtime(
            &mut runtime,
            &json!({
                "operation": "task_retry",
                "data_dir": data_dir,
                "payload": { "task_id": task_id }
            })
            .to_string(),
        )
        .expect("retry download task");

        assert_eq!(retried["task_id"], task_id);
        assert_eq!(retried["status"], "completed");
        assert_eq!(retried["downloaded_file"]["size_bytes"], 11);
        let local_path = retried["downloaded_file"]["local_path"].as_str().unwrap();
        assert_eq!(fs::read(local_path).unwrap(), b"retry bytes");
        let requests = handle.join().unwrap();
        assert_eq!(requests.len(), 2);
        assert!(requests[0].starts_with("GET /source.bin HTTP/1.1"));
        assert!(requests[1].starts_with("GET /source.bin HTTP/1.1"));
    }

    #[test]
    fn runtime_dispatcher_uses_in_memory_runtime_controls() {
        let mut runtime = getter::core::runtime::GetterRuntime::new();
        let action = runtime_operations::issue_action(
            &mut runtime,
            SealedActionPlan {
                package_id: "android/org.fdroid.fdroid".parse().expect("package id"),
                actions: vec![
                    UpdateAction::Download {
                        url: "https://example.invalid/app.apk".to_owned(),
                        file_name: "app.apk".to_owned(),
                    },
                    UpdateAction::Install {
                        installer: "android_package".to_owned(),
                        file: "app.apk".to_owned(),
                    },
                ],
                lua_object: PackageVersionLuaObject {
                    object_id: "lua:android/org.fdroid.fdroid".to_owned(),
                    dependency_digest: "sha256:test".to_owned(),
                },
            },
        );
        let action_id = action["action_id"].as_str().expect("action id");

        let submitted = runtime_operation_with_runtime(
            &mut runtime,
            &json!({
                "operation": "task_submit",
                "payload": { "action_id": action_id }
            })
            .to_string(),
        )
        .expect("submit");
        let task_id = submitted["task_id"].as_str().expect("task id");
        assert_eq!(submitted["status"], "queued");

        runtime_operation_with_runtime(
            &mut runtime,
            &json!({ "operation": "task_start", "payload": { "task_id": task_id } }).to_string(),
        )
        .expect("start");
        let waiting = runtime_operation_with_runtime(
            &mut runtime,
            &json!({
                "operation": "task_complete_download",
                "payload": { "task_id": task_id }
            })
            .to_string(),
        )
        .expect("complete download");
        assert_eq!(waiting["status"], "running");
        assert_eq!(waiting["phase"]["category"], "waiting_user");

        let completed = runtime_operation_with_runtime(
            &mut runtime,
            &json!({
                "operation": "task_user_result",
                "payload": { "task_id": task_id, "result": "accepted" }
            })
            .to_string(),
        )
        .expect("user result");
        assert_eq!(completed["status"], "completed");
    }

    #[test]
    fn runtime_dispatcher_rejects_unknown_operation() {
        let mut runtime = getter::core::runtime::GetterRuntime::new();

        let error = runtime_operation_with_runtime(
            &mut runtime,
            &json!({ "operation": "task_install_result", "payload": {} }).to_string(),
        )
        .unwrap_err();

        let (code, _, detail) = error.parts();
        assert_eq!(code, "runtime.invalid_request");
        assert!(detail.unwrap().contains("unsupported runtime operation"));
    }

    fn fdroid_fixture() -> &'static str {
        r#"<?xml version="1.0" encoding="utf-8"?>
<fdroid>
  <repo name="F-Droid" timestamp="1700000000" url="https://f-droid.org/repo" />
  <application id="org.fdroid.fdroid">
    <name>F-Droid</name>
    <summary>App repository client</summary>
    <package>
      <version>1.20.0</version>
      <versioncode>1020000</versioncode>
      <apkname>org.fdroid.fdroid_1020000.apk</apkname>
      <hash type="sha256">aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa</hash>
      <size>1234567</size>
    </package>
  </application>
</fdroid>
"#
    }

    fn write_static_update_repo(root: &std::path::Path) {
        let package_dir = root.join("android/org.fdroid.fdroid");
        fs::create_dir_all(&package_dir).unwrap();
        fs::write(
            package_dir.join("metadata.jsonc"),
            r#"{
  "type": "android:app",
  "display_name": "F-Droid",
  "android": { "package_name": "org.fdroid.fdroid" }
}"#,
        )
        .unwrap();
        fs::write(package_dir.join("Manifest"), "").unwrap();
        fs::write(
            package_dir.join("9999.lua"),
            r#"#!/bin/upa-lua v1
return package_version {
  updates = {
    {
      version = "1.2.0",
      artifacts = {
        {
          name = "app.apk",
          url = "https://example.invalid/app.apk",
          file_name = "app.apk",
        },
      },
    },
  },
}
"#,
        )
        .unwrap();
    }

    fn serve_one_download_response(body: &'static [u8]) -> (String, thread::JoinHandle<String>) {
        let listener = TcpListener::bind("127.0.0.1:0").unwrap();
        let address = listener.local_addr().unwrap();
        let handle = thread::spawn(move || {
            let (request, mut stream) = accept_request(&listener);
            write_success_response(&mut stream, body);
            request
        });
        (format!("http://{address}/source.bin"), handle)
    }

    fn serve_failing_then_successful_download_response(
        body: &'static [u8],
    ) -> (String, thread::JoinHandle<Vec<String>>) {
        let listener = TcpListener::bind("127.0.0.1:0").unwrap();
        let address = listener.local_addr().unwrap();
        let handle = thread::spawn(move || {
            let (first, mut first_stream) = accept_request(&listener);
            write!(
                first_stream,
                "HTTP/1.1 500 Internal Server Error\r\nContent-Length: 0\r\n\r\n"
            )
            .unwrap();
            let (second, mut second_stream) = accept_request(&listener);
            write_success_response(&mut second_stream, body);
            vec![first, second]
        });
        (format!("http://{address}/source.bin"), handle)
    }

    fn accept_request(listener: &TcpListener) -> (String, std::net::TcpStream) {
        let (mut stream, _) = listener.accept().unwrap();
        let mut request = Vec::new();
        let mut buffer = [0_u8; 1024];
        loop {
            let read = stream.read(&mut buffer).unwrap();
            if read == 0 {
                break;
            }
            request.extend_from_slice(&buffer[..read]);
            if request.windows(4).any(|window| window == b"\r\n\r\n") {
                break;
            }
        }
        (String::from_utf8_lossy(&request).into_owned(), stream)
    }

    fn write_success_response(stream: &mut std::net::TcpStream, body: &[u8]) {
        write!(
            stream,
            "HTTP/1.1 200 OK\r\nContent-Length: {}\r\nContent-Type: application/octet-stream\r\n\r\n",
            body.len()
        )
        .unwrap();
        stream.write_all(body).unwrap();
    }

    #[test]
    fn runtime_notification_queue_is_bounded_and_drained() {
        drain_runtime_notifications().expect("clear queue");
        for index in 0..(MAX_RUNTIME_NOTIFICATION_QUEUE + 1) {
            enqueue_runtime_notification(getter::core::runtime::RuntimeNotification::TaskChanged {
                task: getter::core::runtime::TaskSnapshot {
                    task_id: format!("task-{index}"),
                    package_id: "android/org.fdroid.fdroid".parse().expect("package id"),
                    status: getter::core::runtime::RuntimeTaskStatus::Running,
                    phase: getter::core::runtime::TaskPhase::new(
                        getter::core::runtime::TaskPhaseCategory::Download,
                    ),
                    progress: None,
                    capabilities: getter::core::runtime::TaskCapabilities::default(),
                    current_diagnostic: None,
                    downloaded_file: None,
                    updated_at: index as u64,
                },
            });
        }

        let drained = drain_runtime_notifications().expect("drain notifications");
        let notifications = drained["notifications"].as_array().expect("notifications");

        assert_eq!(notifications.len(), MAX_RUNTIME_NOTIFICATION_QUEUE);
        assert_eq!(notifications[0]["task"]["task_id"], "task-1");
        assert_eq!(notifications.last().unwrap()["task"]["task_id"], "task-64");
        let empty = drain_runtime_notifications().expect("drain empty queue");
        assert_eq!(empty["notifications"].as_array().unwrap().len(), 0);
    }
}
