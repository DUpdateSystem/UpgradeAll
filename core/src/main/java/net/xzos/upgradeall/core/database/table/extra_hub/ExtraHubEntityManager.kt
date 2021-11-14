package net.xzos.upgradeall.core.database.table.extra_hub

import net.xzos.upgradeall.core.database.metaDatabase
import net.xzos.upgradeall.core.database.table.extra_hub.utils.setExtraHubEntity
import net.xzos.upgradeall.core.database.table.extra_hub.utils.toURLReplace
import net.xzos.upgradeall.core.utils.URLReplace

object ExtraHubEntityManager {
    suspend fun getUrlReplace(uuid: String): URLReplace {
        return metaDatabase.extraHubDao().loadByUuid(uuid)?.toURLReplace() ?: getGlobalUrlReplace()
    }

    suspend fun setUrlReplace(uuid: String, urlReplace: URLReplace) {
        val dao = metaDatabase.extraHubDao()
        dao.loadByUuid(uuid)?.apply {
            setExtraHubEntity(this, urlReplace)
            dao.update(this)
        } ?: ExtraHubEntity(uuid).apply {
            setExtraHubEntity(this, urlReplace)
            dao.insert(this)
        }
    }

    suspend fun deleteExtraHub(id: String) {
        metaDatabase.extraHubDao().deleteByUuid(id)
    }

    suspend fun getGlobalUrlReplace(): URLReplace {
        return getGlobalEntity().toURLReplace()
    }

    suspend fun setGlobalUrlReplace(urlReplace: URLReplace) {
        setUrlReplace(GLOBAL, urlReplace)
    }

    private suspend fun getGlobalEntity() = metaDatabase.extraHubDao().loadByUuid(GLOBAL)
}