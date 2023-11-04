extern crate jni;
mod utils;

use jni::objects::{JClass, JObject, JString};
use jni::sys::jboolean;
use jni::JNIEnv;

use getter::provider::*;
use utils::*;

#[no_mangle]
#[allow(non_snake_case)]
pub extern "C" fn Java_net_xzos_upgradeall_getter_NativeLib_checkAppAvailable<'local>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    hub_uuid: JString<'local>,
    id_map: JObject<'local>,
) -> jboolean {
    if let Ok(hub_uuid) = convert_java_str_to_rust(&mut env, &hub_uuid) {
        if let Ok(id_map) = convert_java_map_to_rust(&mut env, &id_map) {
            if let Ok(runtime) = tokio::runtime::Runtime::new() {
                if let Some(result) =
                    runtime.block_on(async { check_app_available(&hub_uuid, &id_map).await })
                {
                    return result as jboolean;
                }
            }
        };
    }
    false as jboolean
}


#[no_mangle]
#[allow(non_snake_case)]
pub extern "C" fn Java_net_xzos_upgradeall_getter_NativeLib_getAppLatestRelease<'local>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    hub_uuid: JString<'local>,
    id_map: JObject<'local>,
) -> JString<'local> {
    if let Ok(hub_uuid) = convert_java_str_to_rust(&mut env, &hub_uuid) {
        if let Ok(id_map) = convert_java_map_to_rust(&mut env, &id_map) {
            if let Ok(runtime) = tokio::runtime::Runtime::new() {
                if let Some(result) = runtime.block_on(async {
                    if let Some(release) = get_latest_release(&hub_uuid, &id_map).await {
                        Some(serde_json::to_string(&release).unwrap_or_default())
                    } else {
                        None
                    }
                }) {
                    return env.new_string(result).unwrap_or_default();
                }
            }
        };
    }
    env.new_string("").unwrap_or_default()
}

#[no_mangle]
#[allow(non_snake_case)]
pub extern "C" fn Java_net_xzos_upgradeall_getter_NativeLib_getAppReleases<'local>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    hub_uuid: JString<'local>,
    id_map: JObject<'local>,
) -> JString<'local> {
    if let Ok(hub_uuid) = convert_java_str_to_rust(&mut env, &hub_uuid) {
        if let Ok(id_map) = convert_java_map_to_rust(&mut env, &id_map) {
            if let Ok(runtime) = tokio::runtime::Runtime::new() {
                if let Some(result) = runtime.block_on(async {
                    if let Some(releases) = get_releases(&hub_uuid, &id_map).await {
                        Some(serde_json::to_string(&releases).unwrap_or_default())
                    } else {
                        None
                    }
                }) {
                    return env.new_string(result).unwrap_or_default();
                }
            }
        };
    }
    env.new_string("").unwrap_or_default()
}
