package net.xzos.upgradeall.data.json.gson

import com.google.gson.annotations.SerializedName

/**
 * base_version: 1
 * uuid:
 * info: {"hub_name": "", "config_version": 1}
 * web_crawler: {"tool": "", "file_path": ""}
 * applications_mode : {"app_url_template":[{"url":"","description":""}]}
 */
data class HubConfig(
        @SerializedName("base_version") var baseVersion: Int = 0,
        @SerializedName("uuid") var uuid: String? = null,
        @SerializedName("info") var info: InfoBean? = null,
        @SerializedName("web_crawler") var webCrawler: WebCrawlerBean? = null,
        @SerializedName("applications_mode") var applicationsMode: ApplicationsModeBean? = null
) {

    /**
     * hub_name:
     * config_version: 1
     * hub_icon_url: ""
     */
    data class InfoBean(
            @SerializedName("hub_name") var hubName: String? = null,
            @SerializedName("config_version") var configVersion: Int = 0,
            @SerializedName("hub_icon_url ") var hubIconUrl: String? = null
    )

    /**
     * tool:
     * file_path:
     */
    data class WebCrawlerBean(
            @SerializedName("tool") var tool: String? = null,
            @SerializedName("file_path") var filePath: String? = null
    )

    data class ApplicationsModeBean(
            @SerializedName("app_url_template") var appUrlTemplate: List<AppUrlTemplateBean> = mutableListOf()
    ) {

        /**
         * url :
         * description :
         */
        data class AppUrlTemplateBean(
                @SerializedName("url") var url: String,
                @SerializedName("description") var description: String
        )
    }

    companion object {
        @Transient
        const val APP_URL_TEMPLATE_APP_PACKAGE_API = "app_package"
    }
}
