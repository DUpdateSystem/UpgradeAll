package net.xzos.upgradeall.core.websdk.api.client_proxy.cloud_config.migration

import org.json.JSONObject

fun hub5to6(oldJson: JSONObject): JSONObject? {
    if (oldJson.getInt("base_version") != 5) return null
    val map = mapOf(
        "base_version" to 6,
        "config_version" to oldJson.getJSONObject("info").getString("config_version"),
        "uuid" to oldJson.getString("uuid"),
        "info" to mapOf(
            "hub_name" to oldJson.getJSONObject("info").getString("hub_name"),
            "hub_icon_url" to "",
        ),
        "target_check_api" to "",
        "api_keywords" to oldJson.getJSONArray("api_keywords"),
        "app_url_templates" to oldJson.getJSONArray("app_url_templates"),
    )
    return JSONObject(map)
}