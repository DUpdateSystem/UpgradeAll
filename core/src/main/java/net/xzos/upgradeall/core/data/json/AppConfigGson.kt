package net.xzos.upgradeall.core.data.json

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import net.xzos.upgradeall.core.database.Converters
import net.xzos.upgradeall.core.database.metaDatabase
import net.xzos.upgradeall.core.database.table.AppEntity

/**
 * base_version: 2
 * config_version: 1
 * uuid:
 * base_hub_uuid:
 * info: {"app_name": "", "app_id": ""}
 */
class AppConfigGson(
    @SerializedName("base_version") val baseVersion: Int? = null,
    @SerializedName("config_version") val configVersion: Int = 0,
    @SerializedName("uuid") val uuid: String? = null,
    @SerializedName("base_hub_uuid") val baseHubUuid: String? = null,
    @SerializedName("info") val info: InfoBean = InfoBean(),
) {

    /**
     * app_name:
     * url:
     */
    class InfoBean(
        @SerializedName("app_name") val appName: String = "null",
        @SerializedName("app_id") var app_id: String? = null,
    ) {
        fun getAppId(): Map<String, String?> {
            return Converters().stringToMap(app_id)
        }

        fun setAppId(map: Map<String, String>) {
            app_id = Converters().fromMapToString(map)
        }
    }

    override fun toString(): String {
        return Gson().toJson(this)
    }
}

suspend fun AppConfigGson.toAppEntity(): AppEntity {
    val appDatabaseList = metaDatabase.appDao().loadAll()
    val appId = info.getAppId()
    for (appDatabase in appDatabaseList) {
        if (appDatabase.appId == appId) {
            appDatabase.name = info.appName
            appDatabase.cloudConfig = this
            return appDatabase
        }
    }
    return AppEntity(0, info.appName, appId, this)
}