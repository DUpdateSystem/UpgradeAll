package net.xzos.upgradeall.core.data_manager

import net.xzos.upgradeall.core.data.config.AppValue
import net.xzos.upgradeall.core.data.database.AppDatabase
import net.xzos.upgradeall.core.data.json.gson.AppConfigGson
import net.xzos.upgradeall.core.data.json.gson.AppDatabaseExtraData
import net.xzos.upgradeall.core.data.json.nongson.ObjectTag
import net.xzos.upgradeall.core.oberver.Observer
import net.xzos.upgradeall.core.server_manager.AppManager
import net.xzos.upgradeall.core.system_api.api.DatabaseApi


object AppDatabaseManager {

    private const val TAG = "AppDatabaseManager"
    private val objectTag = ObjectTag("Core", TAG)

    // 读取 apps 数据库
    internal var appDatabases: List<AppDatabase> = DatabaseApi.appDatabases
        private set

    init {
        /**
         * 刷新数据库
         */
        DatabaseApi.observeForever(object : Observer {
            override fun onChanged(vararg vars: Any): Any? {
                // 更新数据库
                val database = vars[0]
                if (database is AppDatabase) {
                    appDatabases = DatabaseApi.appDatabases
                    AppManager.refreshData(database)
                }
                return null
            }
        })
    }

    fun getDatabase(databaseId: Long? = null, uuid: String? = null): AppDatabase? {
        val databaseList =
                getDatabaseList(
                        databaseId = databaseId,
                        uuid = uuid
                )
        return if (databaseList.isNotEmpty())
            databaseList[0]
        else null
    }

    fun exists(databaseId: Long? = null, uuid: String? = null): Boolean {
        return getDatabase(databaseId = databaseId, uuid = uuid) != null
    }

    internal fun saveDatabase(appDatabase: AppDatabase): Long {
        return DatabaseApi.saveAppDatabase(appDatabase)
    }

    internal fun deleteDatabase(appDatabase: AppDatabase): Boolean {
        return DatabaseApi.deleteAppDatabase(appDatabase)
    }

    /**
     * appConfig: 软件数据库的 json 数据输入
     */
    fun saveAppConfig(appConfigGson: AppConfigGson): AppDatabase? {
        val name = appConfigGson.info.appName
        val url = appConfigGson.info.url ?: ""
        val uuid = appConfigGson.uuid ?: ""
        val hubUuid = appConfigGson.appConfig.hubInfo.hubUuid ?: ""
        // 如果设置了名字与 UUID，则存入数据库
        val appDatabaseExtraData = AppDatabaseExtraData().apply {
            this.cloudAppConfig = appConfigGson
        }
        val targetChecker = appConfigGson.appConfig.targetChecker
        // 修改数据库
        val appDatabase = (getDatabase(
                uuid = uuid
        ) ?: AppDatabase.newInstance()).also {
            it.name = name
            it.url = url
            it.type = AppDatabase.APP_TYPE_TAG
            it.hubUuid = hubUuid
            // 存储 js 代码
            it.extraData = appDatabaseExtraData
            it.targetChecker = targetChecker
        }
        // 将数据存入 RepoDatabase数据库
        return if (appDatabase.save(true)) appDatabase
        else null
    }

    private fun getDatabaseList(
            databaseId: Long? = null, uuid: String? = null,
            hubUuid: String? = null
    ): List<AppDatabase?> {
        return mutableListOf<AppDatabase?>().apply {
            for (appDatabase in appDatabases) {
                val itemUuid = appDatabase.extraData?.cloudAppConfig?.uuid
                if ((databaseId != null && appDatabase.id == databaseId)
                        || (uuid != null && itemUuid == uuid)
                        || (hubUuid != null && appDatabase.hubUuid == hubUuid)
                )
                    this.add(appDatabase)
            }
        }
    }


    fun translateAppConfig(appDatabase: AppDatabase): AppConfigGson {
        val appBaseVersion = AppValue.app_config_version
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
                                hubUuid = appDatabase.hubUuid
                        ),
                        targetChecker = AppConfigGson.AppConfigBean.TargetCheckerBean(
                                api = appDatabase.targetChecker?.api,
                                extraString = appDatabase.targetChecker?.extraString
                        )
                )
        )
    }
}
