package net.xzos.upgradeall.core.manager

import android.database.sqlite.SQLiteConstraintException
import kotlinx.coroutines.runBlocking
import net.xzos.upgradeall.core.database.metaDatabase
import net.xzos.upgradeall.core.database.table.HubEntity
import net.xzos.upgradeall.core.module.Hub

object HubManager {
    private val hubMap: MutableMap<String, Hub> = runBlocking { metaDatabase.hubDao().loadAll() }
            .associateBy({ it.uuid }, { Hub(it) })
            .toMutableMap()

    fun getHubList(): List<Hub> = hubMap.values.toList()

    fun getHub(uuid: String): Hub? = hubMap[uuid]

    suspend fun updateHub(hubDatabase: HubEntity): Boolean {
        val hubDao = metaDatabase.hubDao()
        try {
            hubDao.insert(hubDatabase)
            hubMap[hubDatabase.uuid] = Hub(hubDatabase)
            // TODO: 错误类型判断，并给出 false 返回值
        } catch (ignore: SQLiteConstraintException) {
            hubDao.update(hubDatabase)
        }
        return true
    }

    suspend fun removeHub(hub: Hub) {
        val hubUuid = hub.uuid
        hubMap.remove(hubUuid)
        metaDatabase.hubDao().deleteByUuid(hubUuid)
    }

    fun isEnableApplicationsMode(): Boolean {
        return hubMap.values.any {
            it.isEnableApplicationsMode()
        }
    }
}