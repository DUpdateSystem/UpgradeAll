use jni::objects::{JClass, JObject, JString};
use jni::sys::{jboolean, jobjectArray, JNI_FALSE, JNI_TRUE};
use jni::JNIEnv;
use std::collections::HashMap;
use std::sync::Arc;
use tokio::runtime::Runtime;
use tokio::sync::Mutex;

lazy_static::lazy_static! {
    static ref RUNTIME: Runtime = Runtime::new().expect("Failed to create Tokio runtime");
    static ref PROVIDERS: Arc<Mutex<HashMap<String, ProviderInfo>>> = 
        Arc::new(Mutex::new(HashMap::new()));
}

#[derive(Clone)]
struct ProviderInfo {
    id: String,
    name: String,
    api_keywords: Vec<String>,
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

    let provider_info = ProviderInfo {
        id: provider_id_str.clone(),
        name: name_str,
        api_keywords: keywords,
    };
    
    RUNTIME.block_on(async {
        let mut providers = PROVIDERS.lock().await;
        providers.insert(provider_id_str, provider_info);
    });

    JNI_TRUE
}

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
            .map(|p| p.name.clone())
            .unwrap_or_default()
    });

    env.new_string(name).expect("Failed to create Java string")
}

// Placeholder implementations for other methods
#[no_mangle]
pub extern "C" fn Java_net_xzos_upgradeall_core_provider_ProviderNative_nativeRegisterMagiskProvider<'local>(
    _env: JNIEnv<'local>,
    _: JClass<'local>,
    _provider_id: JString<'local>,
    _name: JString<'local>,
    _repo_url: JString<'local>,
) -> jboolean {
    // Placeholder
    JNI_TRUE
}

#[no_mangle]
pub extern "C" fn Java_net_xzos_upgradeall_core_provider_ProviderNative_nativeCheckApp<'local>(
    _env: JNIEnv<'local>,
    _: JClass<'local>,
    _provider_id: JString<'local>,
    _app_id: JString<'local>,
) -> jboolean {
    // Placeholder
    JNI_TRUE
}

#[no_mangle]
pub extern "C" fn Java_net_xzos_upgradeall_core_provider_ProviderNative_nativeGetLatestRelease<'local>(
    _env: JNIEnv<'local>,
    _: JClass<'local>,
    _provider_id: JString<'local>,
    _app_id: JString<'local>,
    _app_type: JString<'local>,
) -> JObject<'local> {
    // Placeholder
    JObject::null()
}

#[no_mangle]
pub extern "C" fn Java_net_xzos_upgradeall_core_provider_ProviderNative_nativeSetAndroidCallback<'local>(
    _env: JNIEnv<'local>,
    _: JClass<'local>,
    _provider_id: JString<'local>,
    _callback_obj: JObject<'local>,
) -> jboolean {
    // Placeholder
    JNI_TRUE
}

// Helper functions
fn extract_string_array(env: &mut JNIEnv, array: jobjectArray) -> Result<Vec<String>, String> {
    use jni::objects::JObjectArray;
    let array = unsafe { JObjectArray::from_raw(array) };
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