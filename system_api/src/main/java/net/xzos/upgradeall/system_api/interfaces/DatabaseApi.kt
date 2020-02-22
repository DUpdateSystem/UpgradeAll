package net.xzos.upgradeall.system_api.interfaces

import net.xzos.upgradeall.data.database.AppDatabase
import net.xzos.upgradeall.data.database.HubDatabase


interface DatabaseApi {

    val appDatabases: List<AppDatabase>
    val hubDatabases: List<HubDatabase>

    fun saveAppDatabase(appDatabase: AppDatabase): Long
    fun deleteAppDatabase(appDatabase: AppDatabase): Boolean

    fun saveHubDatabase(hubDatabase: HubDatabase): Boolean
    fun deleteHubDatabase(hubDatabase: HubDatabase): Boolean
}