package net.xzos.upgradeall.core.system_api.api

import net.xzos.upgradeall.core.data.database.AppDatabase
import net.xzos.upgradeall.core.data.database.HubDatabase
import net.xzos.upgradeall.core.oberver.Informer
import net.xzos.upgradeall.core.system_api.interfaces.DatabaseApi


object DatabaseApi : Informer {

    private var databaseApiInterface: DatabaseApi? = null

    fun setInterfaces(interfacesClass: DatabaseApi) {
        databaseApiInterface = interfacesClass
    }

    internal val appDatabases: List<AppDatabase>
        get() = databaseApiInterface?.getAppDatabaseList() ?: listOf()

    internal val hubDatabases: List<HubDatabase>
        get() = databaseApiInterface?.getHubDatabaseList() ?: listOf()

    internal fun saveAppDatabase(appDatabase: AppDatabase): Long {
        val database = databaseApiInterface?.saveAppDatabase(appDatabase)?.also { new ->
            if (appDatabase.needRefreshable) {
                notify(new.apply {
                    needRefreshable = true
                })
            }
        }
        return database?.id ?: 0L
    }

    internal fun deleteAppDatabase(appDatabase: AppDatabase): Boolean =
            databaseApiInterface?.deleteAppDatabase(appDatabase)?.also {
                if (appDatabase.needRefreshable)
                    notify(appDatabase)
            } != null

    internal fun saveHubDatabase(hubDatabase: HubDatabase): Boolean =
            databaseApiInterface?.saveHubDatabase(hubDatabase)?.also {
                notify(hubDatabase)
            } != null

    internal fun deleteHubDatabase(hubDatabase: HubDatabase): Boolean =
            databaseApiInterface?.deleteHubDatabase(hubDatabase)?.also {
                notify(hubDatabase)
            } != null

    private fun notify(database: Any) {
        if ((database is AppDatabase) || database is HubDatabase) {
            notifyChanged(database)
        }
    }
}
