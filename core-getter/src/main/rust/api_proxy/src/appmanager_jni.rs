use crate::app_manager::AppManagerFacade;
use getter_appmanager::AppStatus;
use jni::objects::{JClass, JObject, JString, JValue};
use jni::sys::{jboolean, jint, jobjectArray, JNI_FALSE, JNI_TRUE};
use jni::JNIEnv;
use std::collections::HashMap;

// ========== Core AppManager Functions ==========

#[no_mangle]
pub extern "C" fn Java_net_xzos_upgradeall_core_manager_AppManagerNative_nativeAddApp<'local>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    app_id: JString<'local>,
    hub_uuid: JString<'local>,
) -> jboolean {
    let app_id_str = match env.get_string(&app_id) {
        Ok(s) => s.to_string_lossy().to_string(),
        Err(_) => return JNI_FALSE,
    };

    let hub_uuid_str = match env.get_string(&hub_uuid) {
        Ok(s) => s.to_string_lossy().to_string(),
        Err(_) => return JNI_FALSE,
    };

    let app_data = HashMap::new();
    let hub_data = HashMap::new();

    match AppManagerFacade::add_app(app_id_str, hub_uuid_str, app_data, hub_data) {
        Ok(_) => JNI_TRUE,
        Err(_) => JNI_FALSE,
    }
}

#[no_mangle]
pub extern "C" fn Java_net_xzos_upgradeall_core_manager_AppManagerNative_nativeRemoveApp<'local>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    app_id: JString<'local>,
) -> jboolean {
    let app_id_str = match env.get_string(&app_id) {
        Ok(s) => s.to_string_lossy().to_string(),
        Err(_) => return JNI_FALSE,
    };

    match AppManagerFacade::remove_app(&app_id_str) {
        Ok(result) => if result { JNI_TRUE } else { JNI_FALSE },
        Err(_) => JNI_FALSE,
    }
}

#[no_mangle]
pub extern "C" fn Java_net_xzos_upgradeall_core_manager_AppManagerNative_nativeListApps<'local>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
) -> jobjectArray {
    let apps = match AppManagerFacade::list_apps() {
        Ok(apps) => apps,
        Err(_) => vec![],
    };

    create_string_array(&mut env, apps)
}

// ========== Star Management Functions ==========

#[no_mangle]
pub extern "C" fn Java_net_xzos_upgradeall_core_manager_AppManagerNative_nativeSetStar<'local>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    app_id: JString<'local>,
    star: jboolean,
) -> jboolean {
    let app_id_str = match env.get_string(&app_id) {
        Ok(s) => s.to_string_lossy().to_string(),
        Err(_) => return JNI_FALSE,
    };

    let star_bool = star != JNI_FALSE;

    match AppManagerFacade::set_app_star(&app_id_str, star_bool) {
        Ok(_) => JNI_TRUE,
        Err(_) => JNI_FALSE,
    }
}

#[no_mangle]
pub extern "C" fn Java_net_xzos_upgradeall_core_manager_AppManagerNative_nativeIsStarred<'local>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    app_id: JString<'local>,
) -> jboolean {
    let app_id_str = match env.get_string(&app_id) {
        Ok(s) => s.to_string_lossy().to_string(),
        Err(_) => return JNI_FALSE,
    };

    if AppManagerFacade::is_app_starred(&app_id_str) {
        JNI_TRUE
    } else {
        JNI_FALSE
    }
}

#[no_mangle]
pub extern "C" fn Java_net_xzos_upgradeall_core_manager_AppManagerNative_nativeGetStarredApps<'local>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
) -> jobjectArray {
    let starred_apps = match AppManagerFacade::get_starred_apps() {
        Ok(apps) => apps,
        Err(_) => vec![],
    };

    create_string_array(&mut env, starred_apps)
}

// ========== Version Ignore Functions ==========

#[no_mangle]
pub extern "C" fn Java_net_xzos_upgradeall_core_manager_AppManagerNative_nativeSetIgnoreVersion<'local>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    app_id: JString<'local>,
    version: JString<'local>,
) -> jboolean {
    let app_id_str = match env.get_string(&app_id) {
        Ok(s) => s.to_string_lossy().to_string(),
        Err(_) => return JNI_FALSE,
    };

    let version_str = match env.get_string(&version) {
        Ok(s) => s.to_string_lossy().to_string(),
        Err(_) => return JNI_FALSE,
    };

    match AppManagerFacade::set_ignore_version(&app_id_str, &version_str) {
        Ok(_) => JNI_TRUE,
        Err(_) => JNI_FALSE,
    }
}

#[no_mangle]
pub extern "C" fn Java_net_xzos_upgradeall_core_manager_AppManagerNative_nativeGetIgnoreVersion<'local>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    app_id: JString<'local>,
) -> JString<'local> {
    let app_id_str = match env.get_string(&app_id) {
        Ok(s) => s.to_string_lossy().to_string(),
        Err(_) => return env.new_string("").expect("Failed to create empty string"),
    };

    match AppManagerFacade::get_ignore_version(&app_id_str) {
        Ok(Some(version)) => env.new_string(version).expect("Failed to create Java string"),
        _ => env.new_string("").expect("Failed to create empty string"),
    }
}

#[no_mangle]
pub extern "C" fn Java_net_xzos_upgradeall_core_manager_AppManagerNative_nativeIsVersionIgnored<'local>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    app_id: JString<'local>,
    version: JString<'local>,
) -> jboolean {
    let app_id_str = match env.get_string(&app_id) {
        Ok(s) => s.to_string_lossy().to_string(),
        Err(_) => return JNI_FALSE,
    };

    let version_str = match env.get_string(&version) {
        Ok(s) => s.to_string_lossy().to_string(),
        Err(_) => return JNI_FALSE,
    };

    if AppManagerFacade::is_version_ignored(&app_id_str, &version_str) {
        JNI_TRUE
    } else {
        JNI_FALSE
    }
}

#[no_mangle]
pub extern "C" fn Java_net_xzos_upgradeall_core_manager_AppManagerNative_nativeIgnoreAllCurrentVersions<'local>(
    _env: JNIEnv<'local>,
    _: JClass<'local>,
) -> jint {
    match AppManagerFacade::ignore_all_current_versions() {
        Ok(count) => count as jint,
        Err(_) => -1,
    }
}

// ========== App Filtering Functions ==========

#[no_mangle]
pub extern "C" fn Java_net_xzos_upgradeall_core_manager_AppManagerNative_nativeGetAppsByType<'local>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    app_type: JString<'local>,
) -> jobjectArray {
    let type_str = match env.get_string(&app_type) {
        Ok(s) => s.to_string_lossy().to_string(),
        Err(_) => return std::ptr::null_mut(),
    };

    let apps = match AppManagerFacade::get_apps_by_type(&type_str) {
        Ok(apps) => apps,
        Err(_) => vec![],
    };

    create_string_array(&mut env, apps)
}

#[no_mangle]
pub extern "C" fn Java_net_xzos_upgradeall_core_manager_AppManagerNative_nativeGetAppsByStatus<'local>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    status: JString<'local>,
) -> jobjectArray {
    let status_str = match env.get_string(&status) {
        Ok(s) => s.to_string_lossy().to_string(),
        Err(_) => return std::ptr::null_mut(),
    };

    let app_status = parse_app_status(&status_str);
    
    let apps = match AppManagerFacade::get_apps_by_status(app_status) {
        Ok(apps) => apps,
        Err(_) => vec![],
    };

    create_app_status_info_array(&mut env, apps)
}

#[no_mangle]
pub extern "C" fn Java_net_xzos_upgradeall_core_manager_AppManagerNative_nativeGetStarredAppsWithStatus<'local>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
) -> jobjectArray {
    let apps = match AppManagerFacade::get_starred_apps_with_status() {
        Ok(apps) => apps,
        Err(_) => vec![],
    };

    create_app_status_info_array(&mut env, apps)
}

#[no_mangle]
pub extern "C" fn Java_net_xzos_upgradeall_core_manager_AppManagerNative_nativeGetOutdatedAppsFiltered<'local>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
) -> jobjectArray {
    let apps = match AppManagerFacade::get_outdated_apps_filtered() {
        Ok(apps) => apps,
        Err(_) => vec![],
    };

    create_app_status_info_array(&mut env, apps)
}

// ========== Helper Functions ==========

fn create_string_array(env: &mut JNIEnv, strings: Vec<String>) -> jobjectArray {
    let string_class = env.find_class("java/lang/String")
        .expect("Failed to find String class");
    let empty_string = env.new_string("")
        .expect("Failed to create empty string");
    
    let array = env.new_object_array(
        strings.len() as i32,
        &string_class,
        &empty_string,
    ).expect("Failed to create string array");

    for (i, s) in strings.iter().enumerate() {
        let java_string = env.new_string(s)
            .expect("Failed to create Java string");
        env.set_object_array_element(&array, i as i32, java_string)
            .expect("Failed to set array element");
    }

    array.as_raw()
}

fn create_app_status_info_array(env: &mut JNIEnv, apps: Vec<getter_appmanager::AppStatusInfo>) -> jobjectArray {
    let status_info_class = env.find_class("net/xzos/upgradeall/core/data/AppStatusInfo")
        .expect("Failed to find AppStatusInfo class");
    
    let array = env.new_object_array(
        apps.len() as i32,
        &status_info_class,
        JObject::null(),
    ).expect("Failed to create AppStatusInfo array");

    for (i, app_info) in apps.iter().enumerate() {
        let app_id_jstring = env.new_string(&app_info.app_id)
            .expect("Failed to create app_id string");
        
        let status_jstring = env.new_string(format!("{:?}", app_info.status))
            .expect("Failed to create status string");
        
        let current_version = app_info.current_version.as_deref().unwrap_or("");
        let current_version_jstring = env.new_string(current_version)
            .expect("Failed to create current_version string");
        
        let latest_version = app_info.latest_version.as_deref().unwrap_or("");
        let latest_version_jstring = env.new_string(latest_version)
            .expect("Failed to create latest_version string");

        let status_info_obj = env.new_object(
            &status_info_class,
            "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V",
            &[
                JValue::Object(&app_id_jstring),
                JValue::Object(&status_jstring),
                JValue::Object(&current_version_jstring),
                JValue::Object(&latest_version_jstring),
            ],
        ).expect("Failed to create AppStatusInfo object");

        env.set_object_array_element(&array, i as i32, status_info_obj)
            .expect("Failed to set array element");
    }

    array.as_raw()
}

fn parse_app_status(status: &str) -> AppStatus {
    match status {
        "AppPending" => AppStatus::AppPending,
        "AppInactive" => AppStatus::AppInactive,
        "NetworkError" => AppStatus::NetworkError,
        "AppLatest" => AppStatus::AppLatest,
        "AppOutdated" => AppStatus::AppOutdated,
        "AppNoLocal" => AppStatus::AppNoLocal,
        _ => AppStatus::AppPending,
    }
}