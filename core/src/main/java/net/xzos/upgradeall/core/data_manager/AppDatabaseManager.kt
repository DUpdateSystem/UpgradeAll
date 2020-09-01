package net.xzos.upgradeall.core.data_manager

import kotlinx.coroutines.runBlocking
import net.xzos.upgradeall.core.data.config.AppValue
import net.xzos.upgradeall.core.data.database.AppDatabase
import net.xzos.upgradeall.core.data.database.ApplicationsDatabase
import net.xzos.upgradeall.core.data.database.BaseAppDatabase
import net.xzos.upgradeall.core.data.json.gson.AppConfigGson
import net.xzos.upgradeall.core.data.json.gson.PackageIdGson
import net.xzos.upgradeall.core.data.json.nongson.ObjectTag
import net.xzos.upgradeall.core.server_manager.AppManager
import net.xzos.upgradeall.core.system_api.api.DatabaseApi


object AppDatabaseManager {

    private const val TAG = "AppDatabaseManager"
    private val objectTag = ObjectTag("Core", TAG)

    // 读取 apps 数据库
    val appDatabases: HashSet<AppDatabase> = runBlocking {
        DatabaseApi?.getAppDatabaseList()?.toHashSet() ?: hashSetOf()
    }

    val applicationsDatabases: HashSet<ApplicationsDatabase> = runBlocking {
        DatabaseApi?.getApplicationsDatabaseList()?.toHashSet() ?: hashSetOf()
    }

    fun getAppDatabase(databaseId: Long? = null, uuid: String? = null): AppDatabase? {
        val databaseList =
                getAppDatabaseList(
                        databaseId = databaseId,
                        uuid = uuid
                )
        return if (databaseList.isNotEmpty())
            databaseList[0]
        else null
    }

    fun getApplicationsDatabase(databaseId: Long? = null): ApplicationsDatabase? {
        val databaseList = getApplicationsDatabaseList(databaseId)
        return if (databaseList.isNotEmpty())
            databaseList[0]
        else null
    }

    suspend fun insertAppDatabase(database: AppDatabase): Long {
        database.id = DatabaseApi?.insertAppDatabase(database) ?: return 0L
        if (database.id != 0L) {
            appDatabases.addDatabase(database)
        }
        return database.id
    }

    suspend fun updateAppDatabase(database: AppDatabase, refreshData: Boolean = false): Boolean {
        return DatabaseApi?.updateAppDatabase(database)?.also {
            if (it && refreshData) AppManager.setApp(database)
        } ?: false
    }

    suspend fun deleteAppDatabase(database: AppDatabase): Boolean {
        return DatabaseApi?.deleteAppDatabase(database)?.also {
            if (it) appDatabases.removeDatabase(database)
        } ?: false
    }

    suspend fun insertApplicationsDatabase(database: ApplicationsDatabase): Long {
        database.id = DatabaseApi?.insertApplicationsDatabase(database) ?: return 0L
        if (database.id != 0L) {
            applicationsDatabases.addDatabase(database)
        }
        return database.id
    }

    suspend fun updateApplicationsDatabase(database: ApplicationsDatabase, refreshData: Boolean = false): Boolean {
        return DatabaseApi?.updateApplicationsDatabase(database)?.also {
            if (it && refreshData) AppManager.setApplications(database)
        } ?: false
    }

    suspend fun deleteApplicationsDatabase(database: ApplicationsDatabase): Boolean {
        return DatabaseApi?.deleteApplicationsDatabase(database)?.also {
            if (it) applicationsDatabases.removeDatabase(database)
        } ?: false
    }

    suspend fun deleteDatabase(database: BaseAppDatabase): Boolean {
        return when (database) {
            is AppDatabase -> deleteAppDatabase(database)
            is ApplicationsDatabase -> deleteApplicationsDatabase(database)
            else -> false
        }
    }

    /**
     * appConfig: 软件数据库的 json 数据输入
     */
    suspend fun saveAppConfig(appConfigGson: AppConfigGson): AppDatabase? {
        val name = appConfigGson.info.appName
        val url = appConfigGson.info.url ?: ""
        val uuid = appConfigGson.uuid ?: ""
        val hubUuid = appConfigGson.appConfig.hubInfo.hubUuid ?: ""
        // 如果设置了名字与 UUID，则存入数据库
        val targetChecker = appConfigGson.appConfig.targetChecker
        // 修改数据库
        getAppDatabase(
                uuid = uuid
        )?.let {
            it.name = name
            it.url = url
            it.hubUuid = hubUuid
            // 存储 js 代码
            it.packageId = targetChecker
            it.cloudConfig = appConfigGson
            updateAppDatabase(it)
            return it
        } ?: AppDatabase(0, name, hubUuid, url, targetChecker, appConfigGson).run {
            // 将数据存入 RepoDatabase数据库
            insertAppDatabase(this)
            return this
        }
    }

    private fun getAppDatabaseList(
            databaseId: Long? = null, uuid: String? = null
    ): List<AppDatabase> {
        return mutableListOf<AppDatabase>().apply {
            for (appDatabase in appDatabases) {
                val itemUuid = appDatabase.cloudConfig?.uuid
                if ((databaseId != null && appDatabase.id == databaseId)
                        || (uuid != null && itemUuid == uuid)
                )
                    this.add(appDatabase)
            }
        }
    }

    private fun getApplicationsDatabaseList(databaseId: Long? = null): List<ApplicationsDatabase> {
        return mutableListOf<ApplicationsDatabase>().apply {
            for (appDatabase in applicationsDatabases) {
                if (databaseId != null && appDatabase.id == databaseId)
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
                        targetChecker = PackageIdGson(
                                api = appDatabase.packageId?.api,
                                extraString = appDatabase.packageId?.extraString
                        )
                )
        )
    }
}

private fun HashSet<AppDatabase>.addDatabase(database: AppDatabase) {
    this.add(database)
    AppManager.setApp(database)
}

private fun HashSet<AppDatabase>.removeDatabase(database: AppDatabase) {
    this.remove(database)
    AppManager.removeSingleApp(database.id)
}

private fun HashSet<ApplicationsDatabase>.addDatabase(database: ApplicationsDatabase) {
    this.add(database)
    AppManager.setApplications(database)
}

private fun HashSet<ApplicationsDatabase>.removeDatabase(database: ApplicationsDatabase) {
    this.remove(database)
    AppManager.removeApplications(database.id)
}
