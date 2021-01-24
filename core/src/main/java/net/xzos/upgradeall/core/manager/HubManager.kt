package net.xzos.upgradeall.core.manager

import android.database.sqlite.SQLiteConstraintException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import net.xzos.upgradeall.core.database.metaDatabase
import net.xzos.upgradeall.core.database.table.HubEntity
import net.xzos.upgradeall.core.module.Hub
import net.xzos.upgradeall.core.utils.runWithLock

object HubManager {
    private val mutex = Mutex()
    private lateinit var _hubMap: MutableMap<String, Hub>
    private fun getHubMap(): MutableMap<String, Hub> {
        return if (::_hubMap.isInitialized) {
            _hubMap
        } else {
            mutex.runWithLock {
                runBlocking { metaDatabase.hubDao().loadAll() }
                        .associateBy({ it.uuid }, { Hub(it) })
                        .toMutableMap()
                        .apply {
                            _hubMap = this
                        }
            }
        }
    }

    fun getHubList(): List<Hub> = getHubMap().values.toList()

    fun getHub(uuid: String): Hub? = getHubMap()[uuid]

    suspend fun updateHub(hubDatabase: HubEntity): Boolean {
        val hubDao = metaDatabase.hubDao()
        try {
            hubDao.insert(hubDatabase)
            // TODO: 错误类型判断，并给出 false 返回值
        } catch (ignore: SQLiteConstraintException) {
            hubDao.update(hubDatabase)
        }
        getHubMap()[hubDatabase.uuid] = Hub(hubDatabase)
        return true
    }

    suspend fun removeHub(hub: Hub) {
        val hubUuid = hub.uuid
        getHubMap().remove(hubUuid)
        metaDatabase.hubDao().deleteByUuid(hubUuid)
    }
}