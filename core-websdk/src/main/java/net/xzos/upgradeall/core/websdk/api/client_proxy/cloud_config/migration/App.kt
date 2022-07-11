package net.xzos.upgradeall.core.websdk.api.client_proxy.cloud_config.migration

import org.json.JSONObject

fun app1to2(oldJson: JSONObject): JSONObject? {
    fun apiConvert(s: String) = when (s.lowercase()) {
        "app_package" -> "android_app_package"
        "magisk_module" -> "android_magisk_module"
        "shell" -> "android_custom_shell"
        "shell_root" -> "android_custom_shell_root"
        else -> s.lowercase()
    }

    if (oldJson.getInt("base_version") != 1) return null
    val targetCheckerJson = oldJson.getJSONObject("app_config")
        .getJSONObject("target_checker")
    val map = mapOf(
        "base_version" to 2,
        "config_version" to oldJson.getJSONObject("info").getString("config_version"),
        "uuid" to oldJson.getString("uuid"),
        "base_hub_uuid" to oldJson.getJSONObject("app_config")
            .getJSONObject("hub_info")
            .getString("hub_uuid"),
        "info" to mapOf(
            "name" to oldJson.getJSONObject("info").getString("app_name"),
            "url" to oldJson.getJSONObject("info").getString("url"),
            "extra_map" to mapOf(
                apiConvert(targetCheckerJson.getString("api")) to targetCheckerJson.getString("extra_string")
            )
        ),
    )
    return JSONObject(map)
}