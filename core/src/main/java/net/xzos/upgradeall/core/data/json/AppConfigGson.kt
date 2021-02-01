package net.xzos.upgradeall.core.data.json

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import net.xzos.upgradeall.core.database.metaDatabase
import net.xzos.upgradeall.core.database.table.AppEntity
import net.xzos.upgradeall.core.manager.CloudConfigGetter
import net.xzos.upgradeall.core.manager.HubManager
import net.xzos.upgradeall.core.utils.AutoTemplate

/**
 * base_version: 2
 * config_version: 1
 * uuid:
 * base_hub_uuid:
 * info: {"name": "", "desc": "", "url": , "extra_map": ""}
 */
class AppConfigGson(
        @SerializedName("base_version") val baseVersion: Int,
        @SerializedName("config_version") val configVersion: Int,
        @SerializedName("uuid") val uuid: String,
        @SerializedName("base_hub_uuid") val baseHubUuid: String,
        @SerializedName("info") val info: InfoBean,
) {

    /**
     * app_name:
     * url:
     * desc:
     */
    class InfoBean(
            @SerializedName("name") val name: String,
            @SerializedName("url") var url: String,
            @SerializedName("desc") var desc: String?,
            @SerializedName("extra_map") var extraMap: Map<String, String?>,
    )

    override fun toString(): String {
        return Gson().toJson(this)
    }
}

fun AppConfigGson.getAppId(): Map<String, String?>? {
    val hubConfig = CloudConfigGetter.getHubCloudConfig(baseHubUuid) ?: return null
    return info.extraMap.plus(AutoTemplate.urlToAppId(info.url, hubConfig.appUrlTemplates)
            ?: mapOf())
}

suspend fun AppConfigGson.toAppEntity(): AppEntity? {
    val appDatabaseList = metaDatabase.appDao().loadAll()
    val appId = this.getAppId() ?: return null
    for (appDatabase in appDatabaseList) {
        if (appDatabase.appId == appId) {
            appDatabase.name = info.name
            appDatabase.cloudConfig = this
            return appDatabase
        }
    }
    return AppEntity(0, info.name, appId, null, this)
}