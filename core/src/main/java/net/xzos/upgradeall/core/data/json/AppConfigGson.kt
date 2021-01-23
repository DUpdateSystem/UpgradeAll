package net.xzos.upgradeall.core.data.json

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import net.xzos.upgradeall.core.database.Converters
import net.xzos.upgradeall.core.database.metaDatabase
import net.xzos.upgradeall.core.database.table.AppEntity
import net.xzos.upgradeall.core.manager.HubManager
import net.xzos.upgradeall.core.utils.AutoTemplate

/**
 * base_version: 2
 * config_version: 1
 * uuid:
 * base_hub_uuid:
 * info: {"name": "", "url": , "extra_map": ""}
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
     */
    class InfoBean(
            @SerializedName("name") val name: String,
            @SerializedName("url") var url: String,
            @SerializedName("extra_map") private var _extra_map: String,
    ) {

        var extraMap: Map<String, String?>
            get() = Converters().stringToMap(_extra_map)
            set(value) {
                _extra_map = Converters().fromMapToString(value)!!
            }
    }

    override fun toString(): String {
        return Gson().toJson(this)
    }
}

fun AppConfigGson.getAppId(): Map<String, String?>? {
    val hub = HubManager.getHub(baseHubUuid) ?: return null
    return info.extraMap.plus(AutoTemplate.urlToAppId(info.url, hub.hubConfig.appUrlTemplates)
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