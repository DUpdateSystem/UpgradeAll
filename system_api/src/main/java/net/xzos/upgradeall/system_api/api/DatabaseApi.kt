package net.xzos.upgradeall.system_api.api

import net.xzos.upgradeall.data.database.AppDatabase
import net.xzos.upgradeall.data.database.HubDatabase
import net.xzos.upgradeall.system_api.Register
import net.xzos.upgradeall.system_api.interfaces.DatabaseApi


object DatabaseApi : Register(
        listOf(
                net.xzos.upgradeall.system_api.annotations.DatabaseApi.databaseChanged::class.java
        )
) {

    var databaseApiInterface: DatabaseApi? = null

    val appDatabases: List<AppDatabase>
        get() = databaseApiInterface?.getAppDatabaseList() ?: listOf()

    val hubDatabases: List<HubDatabase>
        get() = databaseApiInterface?.getHubDatabaseList() ?: listOf()

    fun saveAppDatabase(appDatabase: AppDatabase) =
            databaseApiInterface?.saveAppDatabase(appDatabase)?.also {
                notify(it)
            } != null

    fun deleteAppDatabase(appDatabase: AppDatabase) =
            databaseApiInterface?.deleteAppDatabase(appDatabase)?.also {
                notify(it)
            } != null

    fun saveHubDatabase(hubDatabase: HubDatabase) =
            databaseApiInterface?.saveHubDatabase(hubDatabase)?.also {
                notify(it)
            } != null

    fun deleteHubDatabase(hubDatabase: HubDatabase) =
            databaseApiInterface?.deleteHubDatabase(hubDatabase)?.also {
                notify(it)
            } != null

    private fun notify(database: Any) {
        if (database is AppDatabase || database is HubDatabase) {
            runFun(
                    net.xzos.upgradeall.system_api.annotations.DatabaseApi.databaseChanged::class.java,
                    database
            )
        }
    }
}
