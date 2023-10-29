extern crate jni;

use jni::objects::{JClass, JString};
use jni::JNIEnv;

use jni::sys::jstring;

#[no_mangle]
#[allow(non_snake_case)]
pub extern "system" fn Java_net_xzos_upgradeall_getter_NativeLib_stringFromJNI<'local>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    input: JString<'local>,
) -> jstring {
    let input: String = env
        .get_string(&input)
        .expect("Couldn't get java string!")
        .into();

    let output = env
        .new_string(format!("Hello, {}!", input))
        .expect("Couldn't create java string!");

    output.into_raw()
}
