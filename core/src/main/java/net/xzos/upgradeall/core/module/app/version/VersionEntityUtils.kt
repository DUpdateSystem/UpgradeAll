package net.xzos.upgradeall.core.module.app.version

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import net.xzos.upgradeall.core.database.metaDatabase
import net.xzos.upgradeall.core.database.table.AppEntity
import net.xzos.upgradeall.core.database.table.extra_app.ExtraAppEntityManager
import net.xzos.upgradeall.core.database.table.isInit
import net.xzos.upgradeall.core.utils.versioning.VersioningUtils

/**
 * 数据库的版本号数据操作包装
 */
class VersionEntityUtils internal constructor(private val appEntity: AppEntity) {

    fun isIgnored(versionNumber: String): Boolean {
        val markedVersionNumber = if (appEntity.isInit())
            appEntity.ignoreVersionNumber
        else runBlocking(Dispatchers.Default) { ExtraAppEntityManager.getMarkVersionNumber(appEntity.appId) }
        return VersioningUtils.compareVersionNumber(versionNumber, markedVersionNumber) == 0
    }

    suspend fun switchIgnoreStatus(versionNumber: String) {
        if (isIgnored(versionNumber)) unignore()
        else ignore(versionNumber)
    }

    /* 忽略这个版本 */
    private suspend fun ignore(versionNumber: String) {
        appEntity.ignoreVersionNumber = versionNumber
        if (appEntity.isInit())
            metaDatabase.appDao().update(appEntity)
        else
            ExtraAppEntityManager.addMarkVersionNumber(appEntity.appId, versionNumber)
    }

    /* 取消忽略这个版本 */
    private suspend fun unignore() {
        if (appEntity.isInit()) {
            appEntity.ignoreVersionNumber = null
            metaDatabase.appDao().update(appEntity)
        } else {
            ExtraAppEntityManager.removeMarkVersionNumber(appEntity.appId)
        }
    }
}
