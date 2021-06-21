package net.xzos.upgradeall.core.module.app.data

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import net.xzos.upgradeall.core.module.Hub
import net.xzos.upgradeall.core.module.app.version_item.Asset
import net.xzos.upgradeall.core.route.ReleaseListItem
import net.xzos.upgradeall.core.utils.wait

internal class DataGetter(private val dataStorage: DataStorage) {

    private val renewMutex get() = dataStorage.renewMutex
    private val hubList get() = dataStorage.hubList

    /* 刷新版本号数据 */
    suspend fun update() {
        if (renewMutex.isLocked)
            renewMutex.wait()
        else {
            doUpdate()
        }
    }

    private suspend fun doUpdate() {
        renewMutex.withLock {
            coroutineScope {
                hubList.forEach {
                    launch {
                        renewVersionList(it)
                    }
                }
            }
        }
    }

    private suspend fun renewVersionList(hub: Hub) {
        hub.getAppReleaseList(dataStorage.appDatabase.appId)?.let {
            setVersionMap(hub, it)
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