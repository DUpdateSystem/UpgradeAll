package net.xzos.upgradeall.core.data.json

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import net.xzos.upgradeall.core.database.metaDatabase
import net.xzos.upgradeall.core.database.table.HubEntity

/**
 * base_version: 6
 * config_version: 1
 * uuid:
 * info: {"hub_name": "", "hub_icon_url": ""}
 * target_check_api:
 * api_keywords: []
 * app_url_templates": []
 */
class HubConfigGson(
    @SerializedName("base_version") val baseVersion: Int = 0,
    @SerializedName("config_version") val configVersion: Int = 0,
    @SerializedName("uuid") val uuid: String = "",
    @SerializedName("info") val info: InfoBean = InfoBean(),
    @SerializedName("api_keywords") val apiKeywords: List<String> = listOf(),
    @SerializedName("app_url_templates") val appUrlTemplates: List<String> = listOf(),
    @SerializedName("target_check_api") val targetCheckApi: String? = null,
) {

    /**
     * hub_name:
     * hub_icon_url: ""
     */
    class InfoBean(
        @SerializedName("hub_name") var hubName: String = "null",
        @SerializedName("hub_icon_url ") var hubIconUrl: String? = null
    )

    override fun toString(): String {
        return Gson().toJson(this)
    }
}

suspend fun HubConfigGson.toHubEntity(): HubEntity {
    return metaDatabase.hubDao().loadByUuid(this.uuid)?.also {
        it.hubConfig = this
    } ?: HubEntity(this.uuid, this, mutableMapOf())
}
