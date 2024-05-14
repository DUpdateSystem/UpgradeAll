extern crate jni;

use getter::rpc::server::run_server_hanging;
use jni::objects::{JClass, JObject, JString, JValue};
use jni::JNIEnv;
use std::sync::mpsc::channel;
use std::thread;

#[no_mangle]
pub extern "C" fn Java_net_xzos_upgradeall_getter_NativeLib_runServer<'local>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    callback: JObject<'local>,
) -> JString<'local> {
    let (url_tx, url_rx) = channel();
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
            let address = "127.0.0.1:0";
            match run_server_hanging(address, |url| {
                url_tx.send(url.to_string()).unwrap();
                Ok(())
            })
            .await
            {
                Ok(_) => completion_tx.send(None).unwrap(), // No error, send completion signal
                Err(e) => {
                    let err_msg = format!("Error running server: {}", e);
                    completion_tx.send(Some(err_msg)).unwrap();
                }
            }
        });
    });
    let url = match url_rx.recv() {
        Ok(url) => url,
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
