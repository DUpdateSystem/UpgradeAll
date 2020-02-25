package net.xzos.upgradeall.android_api

import net.xzos.dupdatesystem.data.database.AppDatabase
import net.xzos.dupdatesystem.data.database.HubDatabase
import net.xzos.dupdatesystem.system_api.interfaces.DatabaseApi


object DatabaseApi {

    var databaseApiInterface: DatabaseApi? = null

    val appDatabases: List<AppDatabase>
        get() = databaseApiInterface?.getAppDatabaseList() ?: listOf()

    val hubDatabases: List<HubDatabase>
        get() = databaseApiInterface?.getHubDatabaseList() ?: listOf()

    fun saveAppDatabase(appDatabase: AppDatabase) =
            databaseApiInterface?.saveAppDatabase(appDatabase)
                    ?: 0L

    fun deleteAppDatabase(appDatabase: AppDatabase) = databaseApiInterface?.deleteAppDatabase(appDatabase)

    fun saveHubDatabase(hubDatabase: HubDatabase) =
            databaseApiInterface?.saveHubDatabase(hubDatabase) ?: 0L

    fun deleteHubDatabase(hubDatabase: HubDatabase) = databaseApiInterface?.deleteHubDatabase(hubDatabase)
}