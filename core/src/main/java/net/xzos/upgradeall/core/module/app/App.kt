package net.xzos.upgradeall.core.module.app

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.xzos.upgradeall.core.database.table.AppEntity
import net.xzos.upgradeall.core.manager.HubManager
import net.xzos.upgradeall.core.module.Hub
import net.xzos.upgradeall.core.route.ReleaseListItem
import net.xzos.upgradeall.core.utils.VersioningUtils.sortVersionNumberList
import net.xzos.upgradeall.core.utils.coroutines.coroutinesMutableMapOf

class App(
        val appDatabase: AppEntity,
        statusRenewedFun: (appStatus: Int) -> Unit = fun(_: Int) {},
) {
    val appId: Map<String, String?> get() = appDatabase.appId
    private val updater = Updater(this, statusRenewedFun)

    val name get() = appDatabase.name
    val hubList get() = HubManager.hubMap.values.filter { it.isValidApp(this) }
    fun getUrl(hub: Hub): String? = hub.getUrl(this)

    fun setStatusRenewedFun(statusRenewedFun: (appStatus: Int) -> Unit) {
        updater.statusRenewedFun = statusRenewedFun
    }

    val versionList: List<Version>
        get() {
            val versionNumberList = sortVersionNumberList(versionMap.keys)
            val list = mutableListOf<Version>()
            for (versionNumber in versionNumberList) {
                list.add(versionMap[versionNumber]!!)
            }
            return list
        }

    private val versionMap = coroutinesMutableMapOf<String, Version>(true)
    private val versionMapLock: Mutex = Mutex()

    suspend fun update() {
        coroutineScope {
            for (hub in hubList)
                launch {
                    renewVersionList(hub)
                }
        }
        getReleaseStatus()
    }

    fun getReleaseStatus(): Int {
        return updater.getUpdateStatus()
    }

    private suspend fun renewVersionList(hub: Hub) {
        hub.getAppReleaseList(this)?.let {
            setVersionMap(hub, it)
        }
    }

    private suspend fun setVersionMap(hub: Hub, releaseList: List<ReleaseListItem>) {
        versionMapLock.withLock {
            releaseList.forEachIndexed { versionIndex, release ->
                val versionNumber = release.versionNumber
                val version = versionMap[versionNumber]
                        ?: Version(versionNumber, mutableListOf()).also {
                            versionMap[versionNumber] = it
                        }
                val asset = Asset(hub, release.changeLog, this,
                        release.assetsList.mapIndexed { assetIndex, assetItem ->
                            FileAsset(
                                    assetItem.fileName,
                                    assetItem.downloadUrl,
                                    assetItem.fileType,
                                    Pair(versionIndex, assetIndex)
                            )
                        })
                version.assetList.add(asset)
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other !is App) {
            return false
        }
        val appUuid = appDatabase.cloudConfig?.uuid
        return if (appUuid != null && appUuid == other.appDatabase.cloudConfig?.uuid)
            true
        else
            super.equals(other)
    }

    override fun hashCode(): Int {
        return (appDatabase.cloudConfig?.uuid ?: appId).hashCode()
    }
}