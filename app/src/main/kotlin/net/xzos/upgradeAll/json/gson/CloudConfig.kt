package net.xzos.upgradeAll.json.gson

import com.google.gson.annotations.SerializedName

data class CloudConfig(
        @SerializedName("list_url") var listUrl: ListUrlBean? = null,
        @SerializedName("app_list") var appList: List<AppListBean> = listOf(),
        @SerializedName("hub_list") var hubList: List<HubListBean> = listOf()
) {
    /**
     * list_url : {"app_list_raw_url":"","hub_list_raw_url":""}
     * app_list : [{"package_name":""}]
     * hub_list : [{"hub_config_file_name":"","hub_config_file_name":""}]
     */

    data class ListUrlBean(
            @SerializedName("app_list_raw_url") var appListRawUrl: String,
            @SerializedName("hub_list_raw_url") var hubListRawUrl: String
    ) {
        /**
         * app_list_raw_url :
         * hub_list_raw_url :
         */
    }

    data class AppListBean(
            @SerializedName("package_name") var packageName: String
    ) {
        /**
         * package_name :
         */
    }

    data class HubListBean(
            @SerializedName("hub_config_name") var hubConfigName: String,
            @SerializedName("hub_config_uuid") var hubConfigUuid: String,
            @SerializedName("hub_config_file_name") var hubConfigFileName: String
    ) {
        /**
         * hub_config_name :
         * hub_config_file_name :
         */
    }
}
