package net.xzos.upgradeall.core.database.table.extra_app

import net.xzos.upgradeall.core.database.metaDatabase


/**
 * 暂时只有忽略版本号功能，所以直接向外提供控制版本号的函数
 */
object ExtraAppEntityManager {
    private val extraAppDao by lazy { metaDatabase.extraAppDao() }
    private suspend fun getExtraAppEntity(appId: Map<String, String?>): ExtraAppEntity? {
        extraAppDao.loadAll().forEach {
            if (it.appId == appId) return it
        }
        return null
    }

    suspend fun getMarkVersionNumber(appId: Map<String, String?>): String? {
        return getExtraAppEntity(appId)?.__mark_version_number
    }

    suspend fun removeMarkVersionNumber(appId: Map<String, String?>) {
        getExtraAppEntity(appId)?.run {
            extraAppDao.delete(this)
        }
    }

    suspend fun addMarkVersionNumber(appId: Map<String, String?>, versionNumber: String) {
        getExtraAppEntity(appId)?.apply {
            this.__mark_version_number = versionNumber
        }?.also {
            extraAppDao.update(it)
        } ?: extraAppDao.insert(ExtraAppEntity(0, appId, versionNumber))
    }
}