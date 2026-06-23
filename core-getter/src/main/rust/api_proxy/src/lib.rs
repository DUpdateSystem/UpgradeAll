extern crate jni;

use getter::rpc::server::run_server_hanging;
#[cfg(target_os = "android")]
use getter::rustls_platform_verifier;
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
    // Initialize Android-hosted Rust platform integrations for future use.
    // https://github.com/rustls/rustls-platform-verifier/tree/3edb4d278215a8603020351b8b519d907a26041f?tab=readme-ov-file#crate-initialization
    #[cfg(target_os = "android")]
    {
        let rustls_context = match env.new_local_ref(&_context) {
            Ok(context) => context,
            Err(e) => {
                return env
                    .new_string(format!("Error creating rustls context ref: {}", e))
                    .expect("Failed to create Java string");
            }
        };
        if let Err(e) = rustls_platform_verifier::android::init_hosted(&mut env, rustls_context) {
            return env
                .new_string(format!("Error initializing certificate verifier: {}", e))
                .expect("Failed to create Java string");
        }

        let platform_context = match env.new_local_ref(&_context) {
            Ok(context) => context,
            Err(e) => {
                return env
                    .new_string(format!(
                        "Error creating platform adapter context ref: {}",
                        e
                    ))
                    .expect("Failed to create Java string");
            }
        };
        if let Err(e) =
            upgradeall_platform_adapter::android::init_with_env(&mut env, platform_context)
        {
            return env
                .new_string(format!("Error initializing platform adapter: {}", e))
                .expect("Failed to create Java string");
        }
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
            return env.new_string(error).expect("Failed to create Java string");
        }
        Err(e) => {
            return env
                .new_string(format!("Error receiving URL from server thread: {}", e))
                .expect("Failed to create Java string");
        }
    };
    let jurl = match env.new_string(url) {
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

    env.new_string("").expect("Failed to create Java string")
}
