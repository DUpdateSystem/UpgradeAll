extern crate jni;

use getter::operations::autogen::{self, AutogenAcceptance, AutogenOperationError};
use getter::operations::legacy_room::{self, LegacyRoomOperationError};
use getter::rpc::server::run_server_hanging;
#[cfg(target_os = "android")]
use getter::rustls_platform_verifier;
use jni::objects::{JObject, JString, JValue};
use jni::JNIEnv;
use serde::Deserialize;
use serde_json::{json, Value};
use std::path::{Path, PathBuf};
use std::sync::mpsc::channel;
use std::thread;
use upgradeall_platform_adapter::InstalledInventoryScanOptions;
#[cfg(target_os = "android")]
use upgradeall_platform_adapter::PlatformAdapter;

const MAIN_DB_FILE: &str = "main.db";
const CACHE_DB_FILE: &str = "cache.db";

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
    acceptance: ApplyInstalledAutogenAcceptance,
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

#[derive(Debug, Default, Deserialize)]
struct ApplyInstalledAutogenAcceptance {
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
        Ok(()) => success_envelope("bridge initialize", json!({ "initialized": true })),
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
    let plan = autogen::build_local_autogen_plan(&db, &inventory)?;
    let mut preview = autogen::installed_preview_json(&request.data_dir, &plan);
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

impl ApplyInstalledAutogenAcceptance {
    fn into_autogen_acceptance(self) -> Result<AutogenAcceptance, BridgeOperationError> {
        match self.mode.as_deref().unwrap_or("all") {
            "all" => Ok(AutogenAcceptance::AcceptAll),
            "packages" => Ok(AutogenAcceptance::Accept(self.package_ids)),
            other => Err(BridgeOperationError::InvalidRequest(format!(
                "unsupported installed autogen acceptance mode '{other}'"
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
    #[error("migration error: {0}")]
    Migration(#[from] LegacyRoomOperationError),
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
            Self::Migration(error) => (
                error.code(),
                error.message(),
                error
                    .detail()
                    .or_else(|| error.report_path().map(|path| path.display().to_string())),
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
            AutogenOperationError::Autogen(detail) => Self::Autogen(detail),
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn packages_acceptance_defaults_to_all() {
        let acceptance = ApplyInstalledAutogenAcceptance::default()
            .into_autogen_acceptance()
            .expect("acceptance");

        assert!(matches!(acceptance, AutogenAcceptance::AcceptAll));
    }

    #[test]
    fn packages_acceptance_preserves_getter_package_ids() {
        let acceptance = ApplyInstalledAutogenAcceptance {
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
}
