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
        get() = databaseApiInterface?.appDatabases ?: listOf()

    val hubDatabases: List<HubDatabase>
        get() = databaseApiInterface?.hubDatabases ?: listOf()

    fun saveAppDatabase(appDatabase: AppDatabase) =
            databaseApiInterface?.saveAppDatabase(appDatabase).also {
                if (it != 0L) notify(appDatabase)
            } ?: 0L

    fun deleteAppDatabase(appDatabase: AppDatabase) =
            databaseApiInterface?.deleteAppDatabase(appDatabase).also {
                if (it == true) notify(appDatabase)
            } ?: false

    fun saveHubDatabase(hubDatabase: HubDatabase) =
            databaseApiInterface?.saveHubDatabase(hubDatabase).also {
                if (it == true) notify(hubDatabase)
            } ?: 0L

    fun deleteHubDatabase(hubDatabase: HubDatabase) =
            databaseApiInterface?.deleteHubDatabase(hubDatabase).also {
                if (it == true) notify(hubDatabase)
            } ?: false

    private fun notify(database: Any) {
        if (database is AppDatabase || database is HubDatabase) {
            runFun(net.xzos.upgradeall.system_api.annotations.DatabaseApi.databaseChanged::class.java, database)
        }
    }
}
