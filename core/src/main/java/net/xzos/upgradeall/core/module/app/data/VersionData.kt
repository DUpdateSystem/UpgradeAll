package net.xzos.upgradeall.core.module.app.data

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.xzos.upgradeall.core.coreConfig
import net.xzos.upgradeall.core.database.table.AppEntity
import net.xzos.upgradeall.core.module.app.version.Version
import net.xzos.upgradeall.core.module.app.version.VersionUtils
import net.xzos.upgradeall.core.module.app.version.versionComparator
import net.xzos.upgradeall.core.module.app.version_item.Asset
import net.xzos.upgradeall.core.utils.data_cache.CacheConfig
import net.xzos.upgradeall.core.utils.data_cache.DataCacheManager

class VersionData internal constructor(appEntity: AppEntity) {
    private val versionUtils = VersionUtils(appEntity)

    /* 版本号数据列表 */
    private var versionList: List<Version> = emptyList()
    private var sorted = false
    private val mutex = Mutex()
    private val dataCache =
        DataCacheManager(CacheConfig(coreConfig.data_expiration_time, null, true))

    fun getCachedHubUuidList() = dataCache.getAll<String>().values

    suspend fun addAsset(assetList: List<Asset>, hubUuid: String) {
        sorted = false
        mutex.withLock {
            val rowVersionList = VersionUtils.cleanAsset(hubUuid, versionList)
            val versionMap = rowVersionList.associateBy { it.name }.toMutableMap()
            val versionList = versionUtils.doAddAsset(assetList, versionMap)
            this.versionList = versionList.filter { it.name.isNotBlank() }
        }
    }

    private suspend fun sortList(): List<Version> {
        if (!sorted)
            mutex.withLock {
                if (sorted) return@withLock  // 优化等待锁后已经排序的情况
                versionList = versionList.sortedWith(versionComparator)
                sorted = true
            }
        return versionList
    }

    fun isLocked() = mutex.isLocked

    fun getVersionList(): List<Version> = runBlocking { sortList() }
}