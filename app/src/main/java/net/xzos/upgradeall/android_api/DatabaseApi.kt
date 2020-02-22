package net.xzos.upgradeall.android_api

import net.xzos.upgradeall.data.database.AppDatabase
import net.xzos.upgradeall.data.database.HubDatabase
import net.xzos.upgradeall.system_api.interfaces.DatabaseApi


object DatabaseApi {

 var databaseApiInterface: DatabaseApi? = null

    val appDatabases: List<AppDatabase>
        get() = databaseApiInterface?.appDatabases ?: listOf()

    val hubDatabases: List<HubDatabase>
        get() = databaseApiInterface?.hubDatabases?: listOf()

    fun saveAppDatabase(appDatabase: AppDatabase) =
            databaseApiInterface?.saveAppDatabase(appDatabase)
                    ?: 0L

    fun deleteAppDatabase(appDatabase: AppDatabase) = databaseApiInterface?.deleteAppDatabase(appDatabase)

    fun saveHubDatabase(hubDatabase: HubDatabase) =
            databaseApiInterface?.saveHubDatabase(hubDatabase) ?: 0L

    fun deleteHubDatabase(hubDatabase: HubDatabase) = databaseApiInterface?.deleteHubDatabase(hubDatabase)
}