package net.xzos.upgradeall.core.module.app

import kotlinx.coroutines.runBlocking
import net.xzos.upgradeall.core.database.metaDatabase
import net.xzos.upgradeall.core.database.table.AppEntity

/**
 * 版本号数据
 */
class Version(
        private val appEntity: AppEntity,
        /* 版本号 */
        val name: String,
        /* 资源列表 */
        val assetList: MutableList<Asset>
) {
    val isIgnored: Boolean get() = name == appEntity.ignoreVersionNumber

    fun switchIgnoreStatus() {
        if (isIgnored)
            unignore()
        else ignore()
    }

    /* 忽略这个版本 */
    fun ignore() {
        runBlocking { metaDatabase.appDao().update(appEntity) }
    }

    /* 取消忽略这个版本 */
    fun unignore() {
        appEntity.ignoreVersionNumber = null
        runBlocking { metaDatabase.appDao().update(appEntity) }
    }
}