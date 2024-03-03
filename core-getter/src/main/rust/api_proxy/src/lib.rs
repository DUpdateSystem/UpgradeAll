extern crate jni;
mod utils;

use jni::objects::{JByteArray, JClass, JObject, JString};
use jni::sys::jboolean;
use jni::JNIEnv;

use getter::api::{check_app_available, get_latest_release, get_releases};
use utils::*;

#[no_mangle]
#[allow(non_snake_case)]
pub extern "C" fn Java_net_xzos_upgradeall_getter_NativeLib_checkAppAvailable<'local>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    hub_uuid: JString<'local>,
    app_data: JObject<'local>,
    hub_data: JObject<'local>,
) -> jboolean {
    if let Ok(hub_uuid) = convert_java_str_to_rust(&mut env, &hub_uuid) {
        if let Ok(app_data) = convert_java_bmap_to_rust(&mut env, &app_data) {
            if let Ok(hub_data) = convert_java_bmap_to_rust(&mut env, &hub_data) {
                if let Ok(runtime) = tokio::runtime::Runtime::new() {
                    if let Some(result) = runtime.block_on(async {
                        let app_data = app_data
                            .iter()
                            .map(|(k, v)| (k.as_str(), v.as_str()))
                            .collect();
                        let hub_data = hub_data
                            .iter()
                            .map(|(k, v)| (k.as_str(), v.as_str()))
                            .collect();
                        check_app_available(&hub_uuid, &app_data, &hub_data).await
                    }) {
                        return result as jboolean;
                    }
                }
            };
        }
    }
    false as jboolean
}

#[no_mangle]
#[allow(non_snake_case)]
pub extern "C" fn Java_net_xzos_upgradeall_getter_NativeLib_getAppLatestRelease<'local>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    hub_uuid: JString<'local>,
    app_data: JObject<'local>,
    hub_data: JObject<'local>,
) -> JByteArray<'local> {
    if let Ok(hub_uuid) = convert_java_str_to_rust(&mut env, &hub_uuid) {
        if let Ok(app_data) = convert_java_bmap_to_rust(&mut env, &app_data) {
            if let Ok(hub_data) = convert_java_bmap_to_rust(&mut env, &hub_data) {
                if let Ok(runtime) = tokio::runtime::Runtime::new() {
                    if let Some(result) = runtime.block_on(async {
                        let app_data = app_data
                            .iter()
                            .map(|(k, v)| (k.as_str(), v.as_str()))
                            .collect();
                        let hub_data = hub_data
                            .iter()
                            .map(|(k, v)| (k.as_str(), v.as_str()))
                            .collect();
                        get_latest_release(&hub_uuid, &app_data, &hub_data).await
                    }) {
                        return env
                            .byte_array_from_slice(result.as_bytes())
                            .unwrap_or_default();
                    }
                }
            }
        };
    }
    env.new_byte_array(0).unwrap_or_default()
}

#[no_mangle]
#[allow(non_snake_case)]
pub extern "C" fn Java_net_xzos_upgradeall_getter_NativeLib_getAppReleases<'local>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    hub_uuid: JString<'local>,
    app_data: JObject<'local>,
    hub_data: JObject<'local>,
) -> JByteArray<'local> {
    if let Ok(hub_uuid) = convert_java_str_to_rust(&mut env, &hub_uuid) {
        if let Ok(app_data) = convert_java_bmap_to_rust(&mut env, &app_data) {
            if let Ok(hub_data) = convert_java_bmap_to_rust(&mut env, &hub_data) {
                if let Ok(runtime) = tokio::runtime::Runtime::new() {
                    if let Some(result) = runtime.block_on(async {
                        let app_data = app_data
                            .iter()
                            .map(|(k, v)| (k.as_str(), v.as_str()))
                            .collect();
                        let hub_data = hub_data
                            .iter()
                            .map(|(k, v)| (k.as_str(), v.as_str()))
                            .collect();
                        get_releases(&hub_uuid, &app_data, &hub_data).await
                    }) {
                        return env
                            .byte_array_from_slice(result.as_bytes())
                            .unwrap_or_default();
                    }
                }
            }
        };
    }
    env.new_byte_array(0).unwrap_or_default()
}
