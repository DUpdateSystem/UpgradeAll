extern crate jni;

mod app_manager;
mod appmanager_jni;
mod provider_jni_simple;

// RPC server is not needed for AppManager JNI bindings
// use getter_rpc::server::run_server_hanging;
#[cfg(target_os = "android")]
use rustls_platform_verifier;
use jni::objects::{JClass, JObject, JString, JValue};
use jni::JNIEnv;
use std::sync::mpsc::channel;
use std::thread;

#[no_mangle]
pub extern "C" fn Java_net_xzos_upgradeall_getter_NativeLib_runServer<'local>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    _context: JObject,
    callback: JObject<'local>,
) -> JString<'local> {
    // Initialize the certificate verifier for future use.
    // https://github.com/rustls/rustls-platform-verifier/tree/3edb4d278215a8603020351b8b519d907a26041f?tab=readme-ov-file#crate-initialization
    #[cfg(target_os = "android")]
    match rustls_platform_verifier::android::init_hosted(&mut env, _context) {
        Ok(_) => {}
        Err(e) => {
            return env
                .new_string(format!("Error initializing certificate verifier: {}", e))
                .expect("Failed to create Java string");
        }
    }
    let (url_tx, url_rx) = channel::<String>();
    let (completion_tx, completion_rx) = channel::<Option<String>>();
    thread::spawn(move || {
        let runtime = match tokio::runtime::Runtime::new() {
            Ok(rt) => rt,
            Err(e) => {
                let err_msg = format!("Error creating Tokio runtime: {}", e);
                completion_tx.send(Some(err_msg)).unwrap();
                return;
            }
        };
        runtime.block_on(async move {
            // RPC server disabled for now - focusing on AppManager JNI
            let err_msg = "RPC server is not implemented in this build".to_string();
            completion_tx.send(Some(err_msg)).unwrap();
        });
    });
    
    // Since RPC server is disabled, return a placeholder URL
    let url = "http://localhost:0/disabled".to_string();
    
    let jurl = match env.new_string(&url) {
        Ok(jurl) => jurl,
        Err(e) => {
            return env
                .new_string(format!("Error creating URL Java string: {}", e))
                .expect("Failed to create Java string");
        }
    };
    let call_result = env.call_method(
        callback,
        "callback",
        "(Ljava/lang/String;)V",
        &[JValue::Object(&jurl)],
    );

    if let Err(e) = call_result {
        return env
            .new_string(format!("JNI call error: {}", e))
            .expect("Failed to create Java string");
    }

    let error = match completion_rx.recv() {
        Ok(error) => error,
        Err(e) => {
            return env
                .new_string(format!("Error receiving error from server thread: {}", e))
                .expect("Failed to create Java string");
        }
    };
    match error {
        None => env.new_string("").expect("Failed to create Java string"),
        Some(error) => env
            .new_string(format!("Error running server: {}", error))
            .expect("Failed to create Java string"),
    }
}
