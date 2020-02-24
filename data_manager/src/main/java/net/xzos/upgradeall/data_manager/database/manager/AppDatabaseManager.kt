package net.xzos.upgradeall.data_manager.database.manager

import com.google.gson.Gson
import net.xzos.upgradeall.data.config.AppConfig
import net.xzos.upgradeall.data.database.AppDatabase
import net.xzos.upgradeall.data.json.gson.AppConfigGson
import net.xzos.upgradeall.data.json.gson.AppDatabaseExtraData
import net.xzos.upgradeall.data.json.nongson.ObjectTag
import net.xzos.upgradeall.system_api.api.DatabaseApi


object AppDatabaseManager {

    private const val TAG = "AppDatabaseManager"
    private val objectTag = ObjectTag("Core", TAG)

    // 读取 apps 数据库
    val appDatabases: List<AppDatabase>
        get() = DatabaseApi.appDatabases

    fun getDatabase(databaseId: Long? = null, uuid: String? = null): AppDatabase? {
        val databaseList = getDatabaseList(databaseId = databaseId, uuid = uuid)
        return if (databaseList.isNotEmpty())
            databaseList[0]
        else null
    }

    fun exists(databaseId: Long? = null, uuid: String? = null): Boolean {
        return getDatabase(databaseId = databaseId, uuid = uuid) != null
    }

    fun saveDatabase(appDatabase: AppDatabase): Boolean {
        return DatabaseApi.saveAppDatabase(appDatabase)
    }

    fun deleteDatabase(appDatabase: AppDatabase): Boolean {
        return DatabaseApi.deleteAppDatabase(appDatabase)
    }

    /**
     * appConfig: 软件数据库的 json 数据输入
     */
    fun setDatabase(appConfigGson: AppConfigGson): AppDatabase? {
        val name = appConfigGson.info?.appName ?: ""
        val url = appConfigGson.info?.url ?: ""
        val uuid = appConfigGson.uuid ?: ""
        val apiUuid = appConfigGson.appConfig?.hubInfo?.hubUuid ?: ""
        // 如果设置了名字与 UUID，则存入数据库
        val appDatabaseExtraData = AppDatabaseExtraData(Gson().toJson(appConfigGson))
        val targetChecker = appConfigGson.appConfig?.targetChecker
        // 修改数据库
        val appDatabase = (getDatabase(uuid = uuid) ?: AppDatabase.newInstance()).also {
            it.name = name
            it.url = url
            it.type = AppDatabase.APP_TYPE_TAG
            it.api_uuid = apiUuid
            // 存储 js 代码
            it.extraData = appDatabaseExtraData
            it.targetChecker = targetChecker
        }
        // 将数据存入 RepoDatabase数据库
        return if (saveDatabase(appDatabase))
            appDatabase
        else null
    }

    private fun getDatabaseList(databaseId: Long? = null, uuid: String? = null,
                                hubUuid: String? = null)
            : List<AppDatabase?> {
        return mutableListOf<AppDatabase?>().apply {
            for (appDatabase in appDatabases) {
                val itemUuid = appDatabase.extraData?.cloudAppConfigGson?.uuid
                if (appDatabase.id == databaseId || itemUuid == uuid || appDatabase.api_uuid == hubUuid)
                    this.add(appDatabase)
            }
        }
    }


    fun translateAppConfig(appDatabase: AppDatabase): AppConfigGson {
        val appBaseVersion = AppConfig.app_config_version
        return AppConfigGson(
                baseVersion = appBaseVersion,
                uuid = null,
                info = AppConfigGson.InfoBean(
                        appName = appDatabase.name,
                        configVersion = 1,
                        url = appDatabase.url
                ),
                appConfig = AppConfigGson.AppConfigBean(
                        hubInfo = AppConfigGson.AppConfigBean.HubInfoBean(
                                hubUuid = appDatabase.api_uuid
                        ),
                        targetChecker = AppConfigGson.AppConfigBean.TargetCheckerBean(
                                api = appDatabase.targetChecker?.api,
                                extraString = appDatabase.targetChecker?.extraString
                        )
                ))
    }
}
