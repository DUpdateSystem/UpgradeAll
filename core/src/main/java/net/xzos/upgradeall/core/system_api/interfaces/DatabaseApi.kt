package net.xzos.upgradeall.core.system_api.interfaces

import net.xzos.upgradeall.core.data.database.AppDatabase
import net.xzos.upgradeall.core.data.database.ApplicationsDatabase
import net.xzos.upgradeall.core.data.database.HubDatabase


interface DatabaseApi {

    suspend fun getAppDatabaseList(): List<AppDatabase>
    suspend fun getApplicationsDatabaseList(): List<ApplicationsDatabase>
    suspend fun getHubDatabaseList(): List<HubDatabase>

    // 返回变更完成的数据库数据
    // 若返回值为 NULL，则操作数据失败
    suspend fun insertAppDatabase(appDatabase: AppDatabase): Long?
    suspend fun updateAppDatabase(appDatabase: AppDatabase): Boolean
    suspend fun deleteAppDatabase(appDatabase: AppDatabase): Boolean
    suspend fun insertApplicationsDatabase(applicationsDatabase: ApplicationsDatabase): Long?
    suspend fun updateApplicationsDatabase(applicationsDatabase: ApplicationsDatabase): Boolean
    suspend fun deleteApplicationsDatabase(applicationsDatabase: ApplicationsDatabase): Boolean
    suspend fun insertHubDatabase(hubDatabase: HubDatabase): Long?
    suspend fun updateHubDatabase(hubDatabase: HubDatabase): Boolean
    suspend fun deleteHubDatabase(hubDatabase: HubDatabase): Boolean
}