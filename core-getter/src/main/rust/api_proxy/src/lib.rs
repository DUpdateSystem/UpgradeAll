extern crate jni;

use getter::operations::autogen::{self, AutogenAcceptance, AutogenOperationError};
use getter::operations::legacy_room::{self, LegacyRoomOperationError};
use getter::operations::read_model::{self, ReadModelOperationError};
use getter::operations::runtime as runtime_operations;
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
            runtime_operations::issue_action_from_registered_package_json(runtime, &db, &payload)
        }
        "task_submit" => runtime_operations::submit_action_json(runtime, &payload),
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
        "task_retry" => runtime_operations::task_retry_json(runtime, &payload),
        "task_remove" => runtime_operations::task_remove_json(runtime, &payload),
        "task_clean" => runtime_operations::task_clean_json(runtime, &payload),
        other => Err(runtime_operations::RuntimeOperationError::InvalidRequest(
            format!("unsupported runtime operation '{other}'"),
        )),
    }
    .map_err(BridgeOperationError::Runtime)
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
    #[error("read model error: {0}")]
    ReadModel(#[from] ReadModelOperationError),
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
            Self::Migration(error) => (
                error.code(),
                error.message(),
                error
                    .detail()
                    .or_else(|| error.report_path().map(|path| path.display().to_string())),
            ),
            Self::ReadModel(error) => (error.code(), error.message(), error.detail()),
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
        repository::{RepositoryMetadata, REPO_API_VERSION_V1},
        runtime::{PackageVersionLuaObject, SealedActionPlan},
        RepositoryPriority, UpdateAction,
    };
    use std::fs;

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
