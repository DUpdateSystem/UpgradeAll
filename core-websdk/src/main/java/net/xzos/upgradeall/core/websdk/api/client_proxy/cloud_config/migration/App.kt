package net.xzos.upgradeall.core.websdk.api.client_proxy.cloud_config.migration

import net.xzos.upgradeall.core.websdk.json.AppConfigGson
import org.json.JSONObject

fun app1to2(oldJson: JSONObject): AppConfigGson? {
    fun apiConvert(s: String) = when (s.lowercase()) {
        "app_package" -> "android_app_package"
        "magisk_module" -> "android_magisk_module"
        "shell" -> "android_custom_shell"
        "shell_root" -> "android_custom_shell_root"
        else -> s.lowercase()
    }

    if (oldJson.optInt("base_version") != 1) return null
    val targetCheckerJson = oldJson.getJSONObject("app_config")
        .getJSONObject("target_checker")
    return AppConfigGson(
        baseVersion = 2,
        configVersion = oldJson.getJSONObject("info").optInt("config_version"),
        uuid = oldJson.optString("uuid"),
        baseHubUuid = oldJson.getJSONObject("app_config")
            .getJSONObject("hub_info")
            .optString("hub_uuid"),
        info = AppConfigGson.InfoBean(
            name = oldJson.getJSONObject("info").optString("app_name"),
            url = oldJson.getJSONObject("info").optString("url"),
            desc = null,
            extraMap = mapOf(
                apiConvert(targetCheckerJson.getString("api")) to targetCheckerJson.optString("extra_string")
            ),
        )
    )
}