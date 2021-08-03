package net.xzos.upgradeall.core.module.app.data

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.xzos.upgradeall.core.module.Hub
import net.xzos.upgradeall.core.module.app.version.Version
import net.xzos.upgradeall.core.module.app.version_item.Asset
import net.xzos.upgradeall.core.route.ReleaseListItem
import net.xzos.upgradeall.core.utils.coroutines.toCoroutinesMutableList
import net.xzos.upgradeall.core.utils.unlockWithCheck
import net.xzos.upgradeall.core.utils.wait

internal class DataGetter(private val dataStorage: DataStorage) {

    private val renewMutex get() = dataStorage.renewMutex
    private val hubList get() = dataStorage.hubList

    /* 刷新版本号数据 */
    suspend fun update() {
        if (renewMutex.isLocked)
            renewMutex.wait()
        else
            renewMutex.withLock {
                doUpdate()
            }
    }

    fun getVersionList(callback: (List<Version>) -> Unit) {
        doGetVersionList(callback)
    }

    private fun doGetVersionList(callback: (List<Version>) -> Unit) {
        val cachedHubUuid = dataStorage.versionData.getCachedHubUuidList()
        val nocacheHubList = hubList.filterNot { cachedHubUuid.contains(it.uuid) }
            .toCoroutinesMutableList(true)
        if (nocacheHubList.isEmpty())
            callback(dataStorage.versionData.getVersionList())
        else
            nocacheHubList.forEach { hub ->
                renewVersionList(hub) {
                    nocacheHubList.remove(hub)
                    if (nocacheHubList.isEmpty())
                        callback(dataStorage.versionData.getVersionList())
                }
            }
    }

    private suspend fun doUpdate() {
        val mutex = Mutex(true)
        getVersionList {
            mutex.unlockWithCheck()
        }
        mutex.wait()
    }

    private fun renewVersionList(hub: Hub, callback: (List<ReleaseListItem>?) -> Unit) {
        hub.getAppReleaseList(dataStorage.appDatabase.appId) {
            it?.let { runBlocking { setVersionMap(hub, it) } }
            callback(it)
        }
    }

    private suspend fun setVersionMap(hub: Hub, releaseList: List<ReleaseListItem>) {
        val assetsList = mutableListOf<Asset>()
        releaseList.forEachIndexed { versionIndex, release ->
            val versionNumber = release.versionNumber
            val asset = Asset.newInstance(
                versionNumber, hub, release.changeLog,
                release.assetsList.mapIndexed { assetIndex, assetItem ->
                    Asset.Companion.TmpFileAsset(
                        assetItem.fileName,
                        assetItem.downloadUrl,
                        assetItem.fileType,
                        Pair(versionIndex, assetIndex)
                    )
                }
            )
            assetsList.add(asset)
        }
        dataStorage.versionData.addAsset(assetsList, hub.uuid)
    }
}