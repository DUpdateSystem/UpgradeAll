package net.xzos.upgradeall.core.module.app.data

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.xzos.upgradeall.core.database.table.AppEntity
import net.xzos.upgradeall.core.module.app.version_item.Asset
import net.xzos.upgradeall.core.module.app.version.Version
import net.xzos.upgradeall.core.module.app.version.VersionUtils
import net.xzos.upgradeall.core.module.app.version.versionComparator

class VersionData internal constructor(appEntity: AppEntity) {
    private val versionUtils = VersionUtils(appEntity)

    /* 版本号数据列表 */
    private var versionList: List<Version> = emptyList()
    private var versionListMutex = Mutex()

    suspend fun addAsset(assetList: List<Asset>, cleanHubUuid: String) {
        versionListMutex.withLock {
            val rowVersionList = VersionUtils.cleanAsset(cleanHubUuid, versionList)
            val versionMap = rowVersionList.map { it.name to it }.toMap().toMutableMap()
            val versionList = versionUtils.doAddAsset(assetList, versionMap)
            val a = versionList.sortedWith(versionComparator)
            this.versionList = a
        }
    }

    fun isLocked() = versionListMutex.isLocked

    fun getVersionList(): List<Version> = versionList
}