use jni::objects::{JClass, JObject, JString, JValue};
use jni::sys::{jboolean, jobjectArray, JNI_FALSE, JNI_TRUE};
use jni::JNIEnv;
use std::collections::HashMap;
use std::sync::Arc;
use tokio::runtime::Runtime;

// Provider JNI implementation will be added when the provider system is ready
// For now, we'll use placeholder implementations

lazy_static::lazy_static! {
    static ref RUNTIME: Runtime = Runtime::new().expect("Failed to create Tokio runtime");
    static ref PROVIDERS: Arc<tokio::sync::Mutex<HashMap<String, Arc<dyn Provider + Send + Sync>>>> = 
        Arc::new(tokio::sync::Mutex::new(HashMap::new()));
}

// ========== Provider Registration ==========

#[no_mangle]
pub extern "C" fn Java_net_xzos_upgradeall_core_provider_ProviderNative_nativeRegisterAndroidProvider<'local>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    provider_id: JString<'local>,
    name: JString<'local>,
    api_keywords: jobjectArray,
) -> jboolean {
    let provider_id_str = match env.get_string(&provider_id) {
        Ok(s) => s.to_string_lossy().to_string(),
        Err(_) => return JNI_FALSE,
    };

    let name_str = match env.get_string(&name) {
        Ok(s) => s.to_string_lossy().to_string(),
        Err(_) => return JNI_FALSE,
    };

    let keywords = match extract_string_array(&mut env, api_keywords) {
        Ok(keywords) => keywords,
        Err(_) => return JNI_FALSE,
    };

    let config = AndroidProviderConfig {
        name: name_str,
        api_keywords: keywords,
        app_url_templates: vec![],
        applications_mode: true,
    };

    let provider = AndroidProvider::new(provider_id_str.clone(), config);
    
    RUNTIME.block_on(async {
        let mut providers = PROVIDERS.lock().await;
        providers.insert(provider_id_str, Arc::new(provider));
    });

    JNI_TRUE
}

#[no_mangle]
pub extern "C" fn Java_net_xzos_upgradeall_core_provider_ProviderNative_nativeRegisterMagiskProvider<'local>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    provider_id: JString<'local>,
    name: JString<'local>,
    repo_url: JString<'local>,
) -> jboolean {
    let provider_id_str = match env.get_string(&provider_id) {
        Ok(s) => s.to_string_lossy().to_string(),
        Err(_) => return JNI_FALSE,
    };

    let name_str = match env.get_string(&name) {
        Ok(s) => s.to_string_lossy().to_string(),
        Err(_) => return JNI_FALSE,
    };

    let repo_url_str = match env.get_string(&repo_url) {
        Ok(s) => s.to_string_lossy().to_string(),
        Err(_) => return JNI_FALSE,
    };

    let config = AndroidProviderConfig {
        name: name_str,
        api_keywords: vec!["android_magisk_module".to_string()],
        app_url_templates: vec![],
        applications_mode: false,
    };

    let provider = MagiskProvider::new(provider_id_str.clone(), config, repo_url_str);
    
    RUNTIME.block_on(async {
        let mut providers = PROVIDERS.lock().await;
        providers.insert(provider_id_str, Arc::new(provider));
    });

    JNI_TRUE
}

// ========== Provider Operations ==========

#[no_mangle]
pub extern "C" fn Java_net_xzos_upgradeall_core_provider_ProviderNative_nativeCheckApp<'local>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    provider_id: JString<'local>,
    app_id: JString<'local>,
) -> jboolean {
    let provider_id_str = match env.get_string(&provider_id) {
        Ok(s) => s.to_string_lossy().to_string(),
        Err(_) => return JNI_FALSE,
    };

    let app_id_str = match env.get_string(&app_id) {
        Ok(s) => s.to_string_lossy().to_string(),
        Err(_) => return JNI_FALSE,
    };

    let result = RUNTIME.block_on(async {
        let providers = PROVIDERS.lock().await;
        if let Some(provider) = providers.get(&provider_id_str) {
            provider.check_app(&app_id_str).await.unwrap_or(false)
        } else {
            false
        }
    });

    if result { JNI_TRUE } else { JNI_FALSE }
}

#[no_mangle]
pub extern "C" fn Java_net_xzos_upgradeall_core_provider_ProviderNative_nativeGetLatestRelease<'local>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    provider_id: JString<'local>,
    app_id: JString<'local>,
    app_type: JString<'local>,
) -> JObject<'local> {
    let provider_id_str = match env.get_string(&provider_id) {
        Ok(s) => s.to_string_lossy().to_string(),
        Err(_) => return JObject::null(),
    };

    let app_id_str = match env.get_string(&app_id) {
        Ok(s) => s.to_string_lossy().to_string(),
        Err(_) => return JObject::null(),
    };

    let app_type_str = match env.get_string(&app_type) {
        Ok(s) => s.to_string_lossy().to_string(),
        Err(_) => return JObject::null(),
    };

    let result = RUNTIME.block_on(async {
        let providers = PROVIDERS.lock().await;
        if let Some(provider) = providers.get(&provider_id_str) {
            let mut app_id_map = HashMap::new();
            app_id_map.insert(app_type_str, app_id_str);
            
            let app = getter_provider::types::App {
                id: app_id_map,
                name: "".to_string(),
                description: None,
                metadata: HashMap::new(),
            };
            
            provider.get_latest_release(&app).await.ok().flatten()
        } else {
            None
        }
    });

    match result {
        Some(release) => create_release_object(&mut env, release),
        None => JObject::null(),
    }
}

// ========== JNI Callbacks from Android ==========

#[no_mangle]
pub extern "C" fn Java_net_xzos_upgradeall_core_provider_ProviderNative_nativeSetAndroidCallback<'local>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    provider_id: JString<'local>,
    callback_obj: JObject<'local>,
) -> jboolean {
    let provider_id_str = match env.get_string(&provider_id) {
        Ok(s) => s.to_string_lossy().to_string(),
        Err(_) => return JNI_FALSE,
    };

    // Store the Java callback object globally
    let callback_global = match env.new_global_ref(callback_obj) {
        Ok(global) => global,
        Err(_) => return JNI_FALSE,
    };

    // Create Rust callback that calls back to Java
    let jni_callback = AndroidJniCallback {
        get_installed_version: Box::new(move |package_name: &str| {
            // This would call back to Java through JNI
            // For now, return a placeholder
            Some("1.0.0".to_string())
        }),
        get_installed_apps: Box::new(|| {
            // This would call back to Java to get installed apps
            vec![]
        }),
        is_app_installed: Box::new(|package_name: &str| {
            // This would check with PackageManager through JNI
            false
        }),
        get_app_info: Box::new(|package_name: &str| {
            // This would get app info from Android through JNI
            None
        }),
    };

    let result = RUNTIME.block_on(async {
        let providers = PROVIDERS.lock().await;
        if let Some(provider) = providers.get(&provider_id_str) {
            // Try to downcast to AndroidProvider
            // This is a simplified version - in production you'd need proper type handling
            true
        } else {
            false
        }
    });

    if result { JNI_TRUE } else { JNI_FALSE }
}

// ========== Helper Functions ==========

fn extract_string_array(env: &mut JNIEnv, array: jobjectArray) -> Result<Vec<String>, String> {
    let len = env.get_array_length(&array).map_err(|e| e.to_string())?;
    let mut strings = Vec::new();
    
    for i in 0..len {
        let elem = env.get_object_array_element(&array, i)
            .map_err(|e| e.to_string())?;
        let jstring = JString::from(elem);
        let string = env.get_string(&jstring)
            .map_err(|e| e.to_string())?
            .to_string_lossy()
            .to_string();
        strings.push(string);
    }
    
    Ok(strings)
}

fn create_release_object<'local>(env: &mut JNIEnv<'local>, release: getter_provider::types::Release) -> JObject<'local> {
    // Create a Java Release object
    let release_class = match env.find_class("net/xzos/upgradeall/core/data/Release") {
        Ok(class) => class,
        Err(_) => return JObject::null(),
    };

    let version_jstring = match env.new_string(&release.version) {
        Ok(s) => s,
        Err(_) => return JObject::null(),
    };

    let name_jstring = match release.name {
        Some(name) => match env.new_string(&name) {
            Ok(s) => s,
            Err(_) => return JObject::null(),
        },
        None => match env.new_string("") {
            Ok(s) => s,
            Err(_) => return JObject::null(),
        },
    };

    match env.new_object(
        &release_class,
        "(Ljava/lang/String;Ljava/lang/String;)V",
        &[
            JValue::Object(&version_jstring),
            JValue::Object(&name_jstring),
        ],
    ) {
        Ok(obj) => obj,
        Err(_) => JObject::null(),
    }
}

// ========== Provider List Operations ==========

#[no_mangle]
pub extern "C" fn Java_net_xzos_upgradeall_core_provider_ProviderNative_nativeListProviders<'local>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
) -> jobjectArray {
    let provider_ids = RUNTIME.block_on(async {
        let providers = PROVIDERS.lock().await;
        providers.keys().cloned().collect::<Vec<String>>()
    });

    create_string_array(&mut env, provider_ids)
}

#[no_mangle]
pub extern "C" fn Java_net_xzos_upgradeall_core_provider_ProviderNative_nativeGetProviderName<'local>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    provider_id: JString<'local>,
) -> JString<'local> {
    let provider_id_str = match env.get_string(&provider_id) {
        Ok(s) => s.to_string_lossy().to_string(),
        Err(_) => return env.new_string("").expect("Failed to create empty string"),
    };

    let name = RUNTIME.block_on(async {
        let providers = PROVIDERS.lock().await;
        providers.get(&provider_id_str)
            .map(|p| p.name().to_string())
            .unwrap_or_default()
    });

    env.new_string(name).expect("Failed to create Java string")
}

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