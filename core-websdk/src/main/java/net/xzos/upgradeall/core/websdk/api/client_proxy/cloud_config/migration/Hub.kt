package net.xzos.upgradeall.core.websdk.api.client_proxy.cloud_config.migration

import net.xzos.upgradeall.core.utils.asSequence
import net.xzos.upgradeall.core.websdk.json.HubConfigGson
import org.json.JSONObject

fun hub5to6(oldJson: JSONObject): HubConfigGson? {
    val versionCodeHubs = listOf(
        "1c010cc9-cff8-4461-8993-a86cd190d377",
        "6a6d590b-1809-41bf-8ce3-7e3f6c8da945",
    )
    if (oldJson.optInt("base_version") != 5) return null
    val uuid = oldJson.optString("uuid")
    return HubConfigGson(
        baseVersion = 6,
        configVersion = oldJson.getJSONObject("info").optInt("config_version"),
        uuid = uuid,
        info = HubConfigGson.InfoBean(
            hubName = oldJson.getJSONObject("info").optString("hub_name"),
            hubIconUrl = null,
        ),
        apiKeywords = oldJson.getJSONArray("api_keywords").asSequence<String>().toList(),
        appUrlTemplates = oldJson.getJSONArray("app_url_templates").asSequence<String>().toList(),
        targetCheckApi = if (uuid in versionCodeHubs) "index" else "version_number",
    )
}