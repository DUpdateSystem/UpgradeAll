package net.xzos.upgradeall.core.system_api.interfaces

import net.xzos.upgradeall.core.data.database.AppDatabase
import net.xzos.upgradeall.core.data.database.HubDatabase


interface DatabaseApi {

    fun getAppDatabaseList(): List<AppDatabase>
    fun getHubDatabaseList(): List<HubDatabase>

    // 返回变更完成的数据库数据
    // 若返回值为 NULL，则操作数据失败
    fun saveAppDatabase(appDatabase: AppDatabase): AppDatabase?

    fun deleteAppDatabase(appDatabase: AppDatabase): AppDatabase?

    fun saveHubDatabase(hubDatabase: HubDatabase): HubDatabase?

    fun deleteHubDatabase(hubDatabase: HubDatabase): HubDatabase?
}