package net.xzos.upgradeAll.data.json.gson

import com.google.gson.annotations.SerializedName

/**
 * base_version: 1
 * uuid:
 * info: {"app_name": "", "config_version": 1, "url": ""}
 * app_config: {"hub_info": {"hub_uuid": ""}, "target_checker": {"api": "", "extra_string": ""}}
 */
data class AppConfig(
        @SerializedName("base_version") var baseVersion: Int = 0,
        @SerializedName("uuid") var uuid: String? = null,
        @SerializedName("info") var info: InfoBean? = null,
        @SerializedName("app_config") var appConfig: AppConfigBean? = null
) {

    /**
     * app_name:
     * config_version: 1
     * url:
     */
    data class InfoBean(
            @SerializedName("app_name") var appName: String? = null,
            @SerializedName("config_version") var configVersion: Int = 0,
            @SerializedName("url") var url: String? = null
    )

    /**
     * hub_info: {"hub_uuid": ""}
     * target_checker: {"api": "", "extra_string": ""}
     */
    data class AppConfigBean(
            @SerializedName("hub_info") var hubInfo: HubInfoBean? = null,
            @SerializedName("target_checker") var targetChecker: TargetCheckerBean? = null
    ) {

        /**
         * hub_uuid:
         */
        data class HubInfoBean(
                @SerializedName("hub_uuid") var hubUuid: String? = null
        )

        /**
         * api:
         * extra_string:
         */
        data class TargetCheckerBean(
                @SerializedName("api") var api: String? = null,
                @SerializedName("extra_string") var extraString: String? = null
        )
    }
}