package net.xzos.upgradeall.core.module.app.data

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.xzos.upgradeall.core.module.Hub
import net.xzos.upgradeall.core.module.app.version.AssetWrapper
import net.xzos.upgradeall.core.module.app.version.Version
import net.xzos.upgradeall.core.module.app.version.VersionWrapper
import net.xzos.upgradeall.core.utils.coroutines.unlockWithCheck
import net.xzos.upgradeall.core.utils.coroutines.wait

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
        hubList.forEach {
            renewVersionList(it)
        }
        callback(dataStorage.versionMap.getVersionList())
    }

    private suspend fun doUpdate() {
        val mutex = Mutex(true)
        getVersionList {
            mutex.unlockWithCheck()
        }
        mutex.wait()
    }

    private fun renewVersionList(hub: Hub) {
        hub.getAppReleaseList(dataStorage)?.mapIndexed { index, releaseGson ->
            VersionWrapper(
                hub, releaseGson,
                releaseGson.assetGsonList.mapIndexed { assetIndex, assetGson ->
                    AssetWrapper(hub, listOf(index, assetIndex), assetGson)
                })
        }?.run {
            dataStorage.versionMap.addReleaseList(this)
        }
    }
}