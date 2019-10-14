package net.xzos.upgradeAll.json.gson

import com.google.gson.annotations.SerializedName

/**
 * base_version : 1
 * uuid :
 * info : {"hub_name":"","config_version":1}
 * web_crawler : {"tool":"","file_path":""}
 */
data class HubConfig(
        @SerializedName("base_version") var baseVersion: Int = 0,
        @SerializedName("uuid") var uuid: String? = null,
        @SerializedName("info") var info: InfoBean = InfoBean(),
        @SerializedName("web_crawler") var webCrawler: WebCrawlerBean = WebCrawlerBean()
) {

    /**
     * hub_name :
     * config_version : 1
     * hub_icon_url : ""
     */
    data class InfoBean(
            @SerializedName("hub_name") var hubName: String? = null,
            @SerializedName("config_version") var configVersion: Int = 0,
            @SerializedName("hub_icon_url ") var hubIconUrl: String? = null
    )

    /**
     * tool :
     * file_path :
     */
    data class WebCrawlerBean(
            @SerializedName("tool") var tool: String? = null,
            @SerializedName("file_path") var filePath: String? = null
    )
}