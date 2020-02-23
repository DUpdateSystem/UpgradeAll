package net.xzos.upgradeall.data_manager.database

import android.util.Log
import net.xzos.upgradeall.data.database.AppDatabase
import net.xzos.upgradeall.data.database.HubDatabase
import net.xzos.upgradeall.data_manager.database.litepal.RepoDatabase
import net.xzos.upgradeall.system_api.interfaces.DatabaseApi
import org.litepal.LitePal
import org.litepal.extension.findAll


object DatabaseManagerApi : DatabaseApi {

    init {
        net.xzos.upgradeall.system_api.api.DatabaseApi.databaseApiInterface = this
    }

    private val nativeAppDatabase: List<RepoDatabase>
        get() = LitePal.findAll()
    private val nativeHubDatabase: List<net.xzos.upgradeall.data_manager.database.litepal.HubDatabase>
        get() = LitePal.findAll()

    override fun getAppDatabaseList(): List<AppDatabase> {
        return nativeAppDatabase.map {
            if (it.type == null) {
                it.type = RepoDatabase.APP_TYPE_TAG
                it.save()
            }
            AppDatabase(it.id, it.name, it.url, it.api_uuid, it.type, it.targetChecker, it.extraData)
        }
    }

    override fun getHubDatabaseList(): List<HubDatabase> {
        return nativeHubDatabase.map {
            HubDatabase(it.name, it.uuid, it.cloudHubConfig, it.extraData)
        }
    }

    override fun saveAppDatabase(appDatabase: AppDatabase): Long {
        var database: RepoDatabase? = null
        for (item in nativeAppDatabase) {
            if (item.id == appDatabase.id) {
                database = item
            }
        }
        if (database == null)
            database = RepoDatabase("", "", "", "")
        return if (database.apply {
                    name = appDatabase.name
                    url = appDatabase.url
                    api_uuid = appDatabase.api_uuid
                    type = appDatabase.type
                    targetChecker = appDatabase.targetChecker
                    extraData = appDatabase.extraData
                }.save())
            database.id
        else 0
    }

    override fun deleteAppDatabase(appDatabase: AppDatabase): Boolean {
        for (database in nativeAppDatabase) {
            if (database.id == appDatabase.id) {
                return database.delete() != 0
            }
        }
        return false
    }

    override fun saveHubDatabase(hubDatabase: HubDatabase): Boolean {
        var database: net.xzos.upgradeall.data_manager.database.litepal.HubDatabase? = null
        for (item in nativeHubDatabase) {
            if (item.uuid == hubDatabase.uuid) {
                database = item
            }
        }
        if (database == null)
            database = net.xzos.upgradeall.data_manager.database.litepal.HubDatabase("", "", "", "")
        return database.apply {
            name = hubDatabase.name
            uuid = hubDatabase.uuid
            cloudHubConfig = hubDatabase.cloudHubConfig
            extraData = hubDatabase.extraData
        }.save()
    }

    override fun deleteHubDatabase(hubDatabase: HubDatabase): Boolean {
        for (database in nativeHubDatabase) {
            if (database.uuid == hubDatabase.uuid) {
                return database.delete() != 0
            }
        }
        return false
    }
}
