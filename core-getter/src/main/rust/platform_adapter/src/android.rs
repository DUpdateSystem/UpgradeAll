//! Android runtime plumbing for Rust-active platform calls.
//!
//! This follows the same shape as rustls-platform-verifier: the Rust native
//! entrypoint initializes JVM/context/classloader handles once, then Rust code
//! can attach a thread and call app classes through the app classloader.

use crate::{
    InstalledInventoryScanOptions, InstalledInventoryScanResult, PlatformAdapter,
    PlatformAdapterError,
};
use jni::objects::{GlobalRef, JClass, JObject, JString, JValue};
use jni::{JNIEnv, JavaVM};
use once_cell::sync::OnceCell;

static RUNTIME: OnceCell<AndroidRuntime> = OnceCell::new();
const INSTALLED_INVENTORY_PROVIDER_CLASS: &str =
    "net.xzos.upgradeall.getter.platform.InstalledInventoryProvider";

struct AndroidRuntime {
    java_vm: JavaVM,
    application_context: GlobalRef,
    class_loader: GlobalRef,
}

/// Initialize Android platform access from a JNI entrypoint.
///
/// `context` should be an Android `Context`. The function stores
/// `context.getApplicationContext()` and its class loader as global refs. It is
/// idempotent for the lifetime of the process.
pub fn init_with_env(
    env: &mut JNIEnv<'_>,
    context: JObject<'_>,
) -> Result<(), PlatformAdapterError> {
    RUNTIME
        .get_or_try_init(|| runtime_from_env(env, context))
        .map(|_| ())
}

fn runtime_from_env(
    env: &mut JNIEnv<'_>,
    context: JObject<'_>,
) -> Result<AndroidRuntime, PlatformAdapterError> {
    let java_vm = env
        .get_java_vm()
        .map_err(|error| PlatformAdapterError::Jni(error.to_string()))?;

    let application_context = env
        .call_method(
            &context,
            "getApplicationContext",
            "()Landroid/content/Context;",
            &[],
        )
        .and_then(|value| value.l())
        .map_err(|error| PlatformAdapterError::Jni(error.to_string()))?;
    let application_context = if application_context.is_null() {
        env.new_global_ref(&context)
    } else {
        env.new_global_ref(&application_context)
    }
    .map_err(|error| PlatformAdapterError::Jni(error.to_string()))?;

    let class_loader = env
        .call_method(
            application_context.as_obj(),
            "getClassLoader",
            "()Ljava/lang/ClassLoader;",
            &[],
        )
        .and_then(|value| value.l())
        .map_err(|error| PlatformAdapterError::Jni(error.to_string()))?;
    let class_loader = env
        .new_global_ref(&class_loader)
        .map_err(|error| PlatformAdapterError::Jni(error.to_string()))?;

    Ok(AndroidRuntime {
        java_vm,
        application_context,
        class_loader,
    })
}

/// Android implementation placeholder for platform capabilities.
#[derive(Debug, Default)]
pub struct AndroidPlatformAdapter;

impl PlatformAdapter for AndroidPlatformAdapter {
    fn scan_installed_inventory(
        &self,
        options: InstalledInventoryScanOptions,
    ) -> Result<InstalledInventoryScanResult, PlatformAdapterError> {
        let options_json = serde_json::to_string(&options).map_err(|error| {
            PlatformAdapterError::MalformedResponse(format!(
                "failed to encode scan options for Android provider: {error}"
            ))
        })?;

        with_attached_env(|env, runtime| {
            let provider_class = JClass::from(load_class(
                env,
                runtime,
                INSTALLED_INVENTORY_PROVIDER_CLASS,
            )?);
            let context = env
                .new_local_ref(runtime.application_context.as_obj())
                .map_err(|error| PlatformAdapterError::Jni(error.to_string()))?;
            let options_json = env
                .new_string(options_json)
                .map_err(|error| PlatformAdapterError::Jni(error.to_string()))?;
            let options_json = JObject::from(options_json);

            let result = env
                .call_static_method(
                    provider_class,
                    "scanInstalledInventory",
                    "(Landroid/content/Context;Ljava/lang/String;)Ljava/lang/String;",
                    &[JValue::Object(&context), JValue::Object(&options_json)],
                )
                .and_then(|value| value.l())
                .map_err(|error| PlatformAdapterError::Jni(error.to_string()))?;
            let result_json = java_string(env, result)?;

            serde_json::from_str(&result_json).map_err(|error| {
                PlatformAdapterError::MalformedResponse(format!(
                    "Android installed inventory provider returned invalid JSON: {error}"
                ))
            })
        })
    }
}

fn with_attached_env<T>(
    f: impl FnOnce(&mut JNIEnv<'_>, &AndroidRuntime) -> Result<T, PlatformAdapterError>,
) -> Result<T, PlatformAdapterError> {
    let runtime = RUNTIME.get().ok_or(PlatformAdapterError::NotInitialized)?;
    let mut env = runtime
        .java_vm
        .attach_current_thread()
        .map_err(|error| PlatformAdapterError::Jni(error.to_string()))?;

    f(&mut env, runtime)
}

/// Load an application class with the app classloader instead of `FindClass`.
fn load_class<'local>(
    env: &mut JNIEnv<'local>,
    runtime: &AndroidRuntime,
    binary_name: &str,
) -> Result<JObject<'local>, PlatformAdapterError> {
    let name = env
        .new_string(binary_name)
        .map_err(|error| PlatformAdapterError::Jni(error.to_string()))?;
    let class = env
        .call_method(
            runtime.class_loader.as_obj(),
            "loadClass",
            "(Ljava/lang/String;)Ljava/lang/Class;",
            &[jni::objects::JValue::Object(&JObject::from(name))],
        )
        .and_then(|value| value.l())
        .map_err(|error| PlatformAdapterError::Jni(error.to_string()))?;
    Ok(class)
}

/// Convert a Java string into a Rust string.
fn java_string(env: &mut JNIEnv<'_>, value: JObject<'_>) -> Result<String, PlatformAdapterError> {
    if value.is_null() {
        return Ok(String::new());
    }
    let value = JString::from(value);
    env.get_string(&value)
        .map(|value| value.into())
        .map_err(|error| PlatformAdapterError::Jni(error.to_string()))
}
