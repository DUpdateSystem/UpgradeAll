use jni::objects::{JMap, JObject, JString};
use jni::JNIEnv;
use std::collections::{HashMap, BTreeMap};

pub fn convert_java_str_to_rust(
    env: &mut JNIEnv,
    string: &JString,
) -> Result<String, jni::errors::Error> {
    Ok(env.get_string(string)?.into())
}

pub fn convert_java_map_to_rust(
    env: &mut JNIEnv,
    map: &JObject,
) -> Result<HashMap<String, String>, jni::errors::Error> {
    let j_map = JMap::from_env(env, &map)?;
    let mut entry_iter = j_map.iter(env)?;

    let mut map = HashMap::new();

    while let Some((key, value)) = entry_iter.next(env)? {
        let key = env.get_string(&JString::from(key))?.into();
        let value = env.get_string(&JString::from(value))?.into();
        map.insert(key, value);
    }
    Ok(map)
}

pub fn convert_java_bmap_to_rust(
    env: &mut JNIEnv,
    map: &JObject,
) -> Result<BTreeMap<String, String>, jni::errors::Error> {
    let j_map = JMap::from_env(env, &map)?;
    let mut entry_iter = j_map.iter(env)?;

    let mut map = BTreeMap::new();

    while let Some((key, value)) = entry_iter.next(env)? {
        let key = env.get_string(&JString::from(key))?.into();
        let value = env.get_string(&JString::from(value))?.into();
        map.insert(key, value);
    }
    Ok(map)
}
