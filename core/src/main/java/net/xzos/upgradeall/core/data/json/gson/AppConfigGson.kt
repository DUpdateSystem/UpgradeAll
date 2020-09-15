package net.xzos.upgradeall.core.data.json.gson

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

/**
 * base_version: 1
 * uuid:
 * info: {"app_name": "", "config_version": 1, "url": ""}
 * app_config: {"hub_info": {"hub_uuid": ""}, "target_checker": {"api": "", "extra_string": ""}}
 */
class AppConfigGson(
        @SerializedName("base_version") var baseVersion: Int? = null,
        @SerializedName("uuid") var uuid: String? = null,
        @SerializedName("info") var info: InfoBean = InfoBean(),
        @SerializedName("app_config") var appConfig: AppConfigBean = AppConfigBean()
) {

    /**
     * app_name:
     * config_version: 1
     * url:
     */
    class InfoBean(
            @SerializedName("app_name") var appName: String = "null",
            @SerializedName("config_version") var configVersion: Int = 0,
            @SerializedName("url") var url: String? = null
    )

    /**
     * hub_info: {"hub_uuid": ""}
     * target_checker: {"api": "", "extra_string": ""}
     */
    class AppConfigBean(
            @SerializedName("hub_info") var hubInfo: HubInfoBean = HubInfoBean(),
            @SerializedName("target_checker") var targetChecker: PackageIdGson = PackageIdGson()
    ) {

        /**
         * hub_uuid:
         */
        class HubInfoBean(
                @SerializedName("hub_uuid") var hubUuid: String? = null
        )
    }

    override fun toString(): String {
        return Gson().toJson(this)
    }
}