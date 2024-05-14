extern crate jni;
mod utils;
mod core;

use jni::objects::{JClass, JObject, JString};
use jni::JNIEnv;
use core::run_server;

#[no_mangle]
pub extern "C" fn Java_net_xzos_upgradeall_getter_NativeLib_runServer<'local>(
    env: JNIEnv<'local>,
    jclass: JClass<'local>,
    callback: JObject<'local>,
) -> JString<'local> {
    return run_server(env, jclass, callback);
}
