package net.xzos.upgradeall.core.module.app.data

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.xzos.upgradeall.core.database.table.AppEntity
import net.xzos.upgradeall.core.module.app.version.Version
import net.xzos.upgradeall.core.module.app.version.VersionUtils
import net.xzos.upgradeall.core.module.app.version.versionComparator
import net.xzos.upgradeall.core.module.app.version_item.Asset
import net.xzos.upgradeall.core.module.network.DataCache

class VersionData internal constructor(appEntity: AppEntity) {
    private val versionUtils = VersionUtils(appEntity)

    /* 版本号数据列表 */
    private var versionList: List<Version> = emptyList()
    private var sorted = false
    private val mutex = Mutex()

    private val hashKey = this.hashCode().toString()
    private fun getHubCacheKey(hubUuid: String) = "$hashKey $hubUuid"
    private fun getHubUuid(hubCacheKey: String) = hubCacheKey.split(" ")[1]

    fun finalize() {
        DataCache.getAnyCache<Set<String>?>(hashKey)?.forEach {
            DataCache.removeAnyCache(it)
        }
        DataCache.removeAnyCache(hashKey)
    }

    fun getCachedHubUuidList():List<String>{
        val uuidList = mutableListOf<String>()
        DataCache.getAnyCache<Set<String>?>(hashKey)?.forEach {
            if (DataCache.getAnyCache<Boolean>(it) == true)
                uuidList.add(getHubUuid(it))
        }
        return uuidList
    }

    suspend fun addAsset(assetList: List<Asset>, hubUuid: String) {
        sorted = false
        mutex.withLock {
            // save cache time
            val hubCacheKey = getHubCacheKey(hubUuid)
            DataCache.cacheAny(hubCacheKey, true)
            val cachedHubUuidList: MutableSet<String> = DataCache.getAnyCache(hashKey)
                ?: mutableSetOf<String>().apply {
                    DataCache.cacheAny(hashKey, this)
                }
            cachedHubUuidList.add(hubCacheKey)

            // process version
            val rowVersionList = VersionUtils.cleanAsset(hubUuid, versionList)
            val versionMap = rowVersionList.map { it.name to it }.toMap().toMutableMap()
            val versionList = versionUtils.doAddAsset(assetList, versionMap)
            this.versionList = versionList
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