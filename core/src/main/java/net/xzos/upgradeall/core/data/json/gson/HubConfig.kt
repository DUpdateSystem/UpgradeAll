package net.xzos.upgradeall.core.data.json.gson

import com.google.gson.annotations.SerializedName

/**
 * base_version: 1
 * uuid:
 * info: {"hub_name": "", "config_version": 1}
 * api_keywords: []
 * app_url_templates": []
 */
class HubConfig(
        @SerializedName("base_version") var baseVersion: Int = 0,
        @SerializedName("uuid") var uuid: String? = null,
        @SerializedName("info") var info: InfoBean = InfoBean(),
        @SerializedName("api_keywords") var apiKeywords: List<String> = listOf(),
        @SerializedName("app_url_templates") var appUrlTemplates: List<String> = listOf()
) {

    /**
     * hub_name:
     * config_version: 1
     * hub_icon_url: ""
     */
    class InfoBean(
            @SerializedName("hub_name") var hubName: String = "null",
            @SerializedName("config_version") var configVersion: Int = 0,
            @SerializedName("hub_icon_url ") var hubIconUrl: String? = null
    )
}
