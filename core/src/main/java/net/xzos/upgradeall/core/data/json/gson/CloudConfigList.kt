package net.xzos.upgradeall.core.data.json.gson

import com.google.gson.annotations.SerializedName

/**
 * app_list: [<AppConfig>]
 * hub_list: [<HubConfig>]
 */
data class CloudConfigList(
        @SerializedName("app_config_list") val appList: List<AppConfigGson> = listOf(),
        @SerializedName("hub_config_list") val hubList: List<HubConfigGson> = listOf()
)
