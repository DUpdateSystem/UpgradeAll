package net.xzos.upgradeall.android_api

import net.xzos.upgradeall.core.data.database.AppDatabase
import net.xzos.upgradeall.core.data.database.AppDatabase.Companion.APP_TYPE_TAG
import net.xzos.upgradeall.core.data.database.HubDatabase
import net.xzos.upgradeall.core.system_api.interfaces.DatabaseApi
import net.xzos.upgradeall.data.database.RepoDatabase
import org.litepal.LitePal
import org.litepal.extension.findAll


object DatabaseApi : DatabaseApi {

    init {
        net.xzos.upgradeall.core.system_api.api.DatabaseApi.setInterfaces(this)
    }

    private val nativeAppDatabase: List<RepoDatabase>
        get() = LitePal.findAll()
    private val nativeHubDatabase: List<net.xzos.upgradeall.data.database.HubDatabase>
        get() = LitePal.findAll()

    override fun getAppDatabaseList(): List<AppDatabase> {
        return nativeAppDatabase.map {
            // 新加的属性，@version: 0.1.1-alpha.8（TODO: 两个大版本后移除）
            if (it.type.isNullOrBlank()) {
                it.type = APP_TYPE_TAG
                it.save()
            }
            conversionAppDatabase(it)
        }
    }

    override fun getHubDatabaseList(): List<HubDatabase> {
        return nativeHubDatabase.map {
            HubDatabase(it.uuid, it.hubConfig)
        }
    }

    override fun saveAppDatabase(appDatabase: AppDatabase): AppDatabase? {
        var database: RepoDatabase? = null
        for (item in nativeAppDatabase) {
            if (item.id == appDatabase.id) {
                database = item
            }
        }
        if (database == null)
            database = RepoDatabase("", "", "", "")
        database.run {
            name = appDatabase.name
            url = appDatabase.url
            api_uuid = appDatabase.hubUuid
            type = appDatabase.type
            targetChecker = appDatabase.targetChecker
            extraData = appDatabase.extraData
        }
        return if (database.save())
            conversionAppDatabase(database)
        else null
    }

    override fun deleteAppDatabase(appDatabase: AppDatabase): AppDatabase? {
        var database: AppDatabase? = null
        for (item in nativeAppDatabase) {
            if (item.id == appDatabase.id) {
                database = conversionAppDatabase(item)
                item.delete()
            }
        }
        return database
    }

    override fun saveHubDatabase(hubDatabase: HubDatabase): HubDatabase? {
        var database: net.xzos.upgradeall.data.database.HubDatabase? = null
        for (item in nativeHubDatabase) {
            if (item.uuid == hubDatabase.uuid) {
                database = item
            }
        }
        if (database == null)
            database = net.xzos.upgradeall.data.database.HubDatabase("", "")
        database.run {
            uuid = hubDatabase.uuid
            hubConfig = hubDatabase.hubConfig
        }
        return if (database.save())
            conversionHubDatabase(database)
        else null
    }

    override fun deleteHubDatabase(hubDatabase: HubDatabase): HubDatabase? {
        var database: HubDatabase? = null
        for (item in nativeHubDatabase) {
            if (item.uuid == hubDatabase.uuid) {
                database = conversionHubDatabase(item)
                item.delete()
            }
        }
        return database
    }

    // 本机跟踪项数据库转换通用格式数据库
    private fun conversionAppDatabase(appDatabase: RepoDatabase): AppDatabase {
        return AppDatabase(appDatabase.id,
                appDatabase.name, appDatabase.url, appDatabase.api_uuid, appDatabase.type,
                appDatabase.targetChecker, appDatabase.extraData)
    }

    // 本机软件源数据库转换通用格式数据库
    private fun conversionHubDatabase(hubDatabase: net.xzos.upgradeall.data.database.HubDatabase): HubDatabase {
        return HubDatabase(hubDatabase.uuid, hubDatabase.hubConfig)
    }
}
