package net.xzos.upgradeall.core.database.table.extra_hub

import net.xzos.upgradeall.core.database.metaDatabase
import net.xzos.upgradeall.core.database.table.extra_hub.utils.setExtraHubEntity
import net.xzos.upgradeall.core.database.table.extra_hub.utils.toURLReplace
import net.xzos.upgradeall.core.utils.URLReplaceData

object ExtraHubEntityManager {
    suspend fun getExtraHub(id: String?) = metaDatabase.extraHubDao().loadByUuid(id ?: GLOBAL)

    suspend fun deleteExtraHub(id: String) = metaDatabase.extraHubDao().deleteByUuid(id)

    suspend fun getUrlReplace(uuid: String): URLReplaceData {
        return metaDatabase.extraHubDao().loadByUuid(uuid)?.toURLReplace() ?: getGlobalUrlReplace()
    }

    suspend fun setUrlReplace(
        uuid: String?,
        enableGlobal: Boolean,
        urlReplaceData: URLReplaceData
    ) {
        val id = uuid ?: GLOBAL
        val dao = metaDatabase.extraHubDao()
        dao.loadByUuid(id)?.apply {
            setExtraHubEntity(this, enableGlobal, urlReplaceData)
            dao.update(this)
        } ?: ExtraHubEntity(id).apply {
            setExtraHubEntity(this, enableGlobal, urlReplaceData)
            dao.insert(this)
        }
    }

    suspend fun getGlobalUrlReplace(): URLReplaceData {
        return getGlobalEntity().toURLReplace()
    }

    private suspend fun getGlobalEntity() = metaDatabase.extraHubDao().loadByUuid(GLOBAL)
}