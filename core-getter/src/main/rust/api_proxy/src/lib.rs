extern crate jni;
mod utils;

use getter::rpc::server::run_server;
use jni::objects::{JClass, JObject, JString, JValue};
use jni::JNIEnv;

#[no_mangle]
#[allow(non_snake_case)]
pub extern "C" fn Java_net_xzos_upgradeall_getter_NativeLib_runServer<'local>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    callback: JObject<'local>,
) -> JString<'local> {
    let runtime = match tokio::runtime::Runtime::new() {
        Ok(rt) => rt,
        Err(e) => {
            return env
                .new_string(format!("Error creating Tokio runtime: {}", e))
                .unwrap()
        }
    };

    let result = runtime.block_on(async {
        run_server("")
            .await
            .map_err(|e| format!("run server error: {}", e))
    });

    // 处理结果，捕获错误或继续操作
    let (url, handle) = match result {
        Ok((url, handle)) => (url, handle),
        Err(e) => return env.new_string(e).unwrap(),
    };

    let jurl = match env.new_string(url) {
        Ok(s) => s,
        Err(e) => {
            return env
                .new_string(format!("Error creating Java string from URL: {}", e))
                .unwrap()
        }
    };

    let call_result = env.call_method(
        callback,
        "callback",
        "(Ljava/lang/String;)V",
        &[JValue::Object(&jurl)],
    );

    // 处理Java方法调用结果
    if let Err(e) = call_result {
        return env.new_string(format!("JNI call error: {}", e)).unwrap();
    }

    runtime.block_on(handle.stopped());

    env.new_string("").unwrap()
}
