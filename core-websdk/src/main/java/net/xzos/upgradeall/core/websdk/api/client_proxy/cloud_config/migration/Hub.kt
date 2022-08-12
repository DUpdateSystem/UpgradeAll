package net.xzos.upgradeall.core.websdk.api.client_proxy.cloud_config.migration

import org.json.JSONObject

fun hub5to6(oldJson: JSONObject): JSONObject? {
    val versionCodeHubs = listOf(
        "1c010cc9-cff8-4461-8993-a86cd190d377",
        "6a6d590b-1809-41bf-8ce3-7e3f6c8da945",
    )
    if (oldJson.optInt("base_version") != 5) return null
    val uuid = oldJson.optString("uuid")
    val map = mapOf(
        "base_version" to 6,
        "config_version" to oldJson.getJSONObject("info").optString("config_version"),
        "uuid" to uuid,
        "info" to mapOf(
            "hub_name" to oldJson.getJSONObject("info").optString("hub_name"),
            "hub_icon_url" to "",
        ),
        "target_check_api" to if (uuid in versionCodeHubs) "index" else "version_number",
        "api_keywords" to oldJson.optJSONArray("api_keywords"),
        "app_url_templates" to oldJson.optJSONArray("app_url_templates"),
    )
    return JSONObject(map)
}