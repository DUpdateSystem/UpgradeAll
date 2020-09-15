package net.xzos.upgradeall.android_api

import android.database.sqlite.SQLiteConstraintException
import net.xzos.upgradeall.core.data.coroutines.coroutinesMutableListOf
import net.xzos.upgradeall.core.data.coroutines.toCoroutinesMutableList
import net.xzos.upgradeall.core.data.database.AppDatabase
import net.xzos.upgradeall.core.data.database.ApplicationsDatabase
import net.xzos.upgradeall.core.data.database.HubDatabase
import net.xzos.upgradeall.core.system_api.api.DatabaseApi
import net.xzos.upgradeall.core.system_api.interfaces.DatabaseApi
import net.xzos.upgradeall.data.database.metaDatabase
import net.xzos.upgradeall.data.database.table.AppEntity
import net.xzos.upgradeall.data.database.table.ApplicationsEntity
import net.xzos.upgradeall.data.database.table.HubEntity


object DatabaseApi : DatabaseApi {

    init {
        DatabaseApi = this
    }

    override suspend fun getAppDatabaseList(): List<AppDatabase> {
        return metaDatabase.appDao().loadAll().map {
            it.toAppDatabase()
        }
    }

    override suspend fun getApplicationsDatabaseList(): List<ApplicationsDatabase> {
        return metaDatabase.applicationsDao().loadAll().map {
            it.toApplicationsDatabase()
        }
    }

    override suspend fun getHubDatabaseList(): List<HubDatabase> {
        return metaDatabase.hubDao().loadAll().map {
            it.toHubDatabase()
        }
    }

    override suspend fun insertAppDatabase(appDatabase: AppDatabase): Long? {
        val rowId = try {
            metaDatabase.appDao().insert(appDatabase.toAppEntity())
        } catch (ignore: SQLiteConstraintException) {
            return null
        }
        return metaDatabase.appDao().getIdByRowId(rowId)
    }

    override suspend fun updateAppDatabase(appDatabase: AppDatabase): Boolean {
        metaDatabase.appDao().update(appDatabase.toAppEntity())
        return true
    }

    override suspend fun deleteAppDatabase(appDatabase: AppDatabase): Boolean {
        metaDatabase.appDao().deleteById(appDatabase.id)
        return true
    }

    override suspend fun insertApplicationsDatabase(applicationsDatabase: ApplicationsDatabase): Long? {
        val rowId = try {
            metaDatabase.applicationsDao().insert(applicationsDatabase.toApplicationsEntity())
        } catch (ignore: SQLiteConstraintException) {
            return null
        }
        return metaDatabase.applicationsDao().getIdByRowId(rowId)
    }

    override suspend fun updateApplicationsDatabase(applicationsDatabase: ApplicationsDatabase): Boolean {
        metaDatabase.applicationsDao().update(applicationsDatabase.toApplicationsEntity())
        return true
    }

    override suspend fun deleteApplicationsDatabase(applicationsDatabase: ApplicationsDatabase): Boolean {
        metaDatabase.applicationsDao().deleteById(applicationsDatabase.id)
        return true
    }

    override suspend fun insertHubDatabase(hubDatabase: HubDatabase): Long? {
        return metaDatabase.hubDao().insert(hubDatabase.toHubEntity())
    }

    override suspend fun updateHubDatabase(hubDatabase: HubDatabase): Boolean {
        metaDatabase.hubDao().update(hubDatabase.toHubEntity())
        return true
    }

    override suspend fun deleteHubDatabase(hubDatabase: HubDatabase): Boolean {
        metaDatabase.hubDao().deleteByUuid(hubDatabase.uuid)
        return true
    }
}

private fun ApplicationsDatabase.toApplicationsEntity(): ApplicationsEntity =
        ApplicationsEntity(id, name, hubUuid, auth, extraId, invalidPackageList, ignoreApps)

private fun ApplicationsEntity.toApplicationsDatabase(): ApplicationsDatabase =
        ApplicationsDatabase(id, name, hubUuid,
                auth ?: mapOf(), extraId ?: mapOf(),
                invalidPackageList?.toCoroutinesMutableList(true) ?: coroutinesMutableListOf(true),
                ignoreApps?.toCoroutinesMutableList(true) ?: coroutinesMutableListOf(true))

// 本机跟踪项数据库转换通用格式数据库
private fun AppEntity.toAppDatabase(): AppDatabase =
        AppDatabase(id, name, hubUuid, url, packageId, cloudConfig,
                auth ?: mapOf(), extraId ?: mapOf(), ignoreVersionNumber)

private fun AppDatabase.toAppEntity(): AppEntity =
        AppEntity(id, name, hubUuid, auth, extraId, url, packageId, ignoreVersionNumber, cloudConfig)

// 本机软件源数据库转换通用格式数据库
private fun HubEntity.toHubDatabase(): HubDatabase = HubDatabase(uuid, hubConfig)

private fun HubDatabase.toHubEntity(): HubEntity = HubEntity(uuid, hubConfig)
