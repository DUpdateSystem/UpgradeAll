package net.xzos.upgradeall.core.module.app

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.xzos.upgradeall.core.database.table.AppEntity
import net.xzos.upgradeall.core.database.table.getEnableSortHubList
import net.xzos.upgradeall.core.manager.HubManager
import net.xzos.upgradeall.core.module.Hub
import net.xzos.upgradeall.core.route.ReleaseListItem
import net.xzos.upgradeall.core.utils.wait

class App(
        val appDatabase: AppEntity,
        statusRenewedFun: (appStatus: Int) -> Unit = fun(_: Int) {},
) {
    val updater = Updater(this, statusRenewedFun)
    private val versionUtils = VersionUtils(this.appDatabase)
    private val renewMutex = Mutex()

    /* App 对象的属性字典 */
    val appId: Map<String, String?> get() = appDatabase.appId

    /* App 名称 */
    val name get() = appDatabase.name

    /* 这个 App 可用的软件源 */
    val hubList get() = appDatabase.getEnableSortHubList().filter { it.isValidApp(this) }
    val isActive: Boolean
        get() {
            val appId = appId
            for (hub in hubList) {
                if (hub.isInactiveApp(appId))
                    return false
            }
            return true
        }

    /* App 在本地的版本号 */
    val installedVersionNumber: String? get() = updater.getInstalledVersionNumber()

    /* 获取相应软件源的网址 */
    fun getUrl(hubUuid: String): String? = HubManager.getHub(hubUuid)?.getUrl(this)

    /* 设置 App 版本号数据刷新时的回调函数 */
    fun setStatusRenewedFun(statusRenewedFun: (appStatus: Int) -> Unit) {
        updater.statusRenewedFun = statusRenewedFun
    }

    /* 清除 App 版本号数据刷新时的回调函数 */
    fun renewStatusRenewedFun() {
        updater.statusRenewedFun = fun(_: Int) {}
    }

    /* 版本号数据列表 */
    val versionList: List<Version> get() = versionUtils.getVersionList()

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
            getReleaseStatus()
        }
    }

    suspend fun getReleaseStatusWaitRenew(): Int {
        renewMutex.wait()
        return getReleaseStatus()
    }

    fun isRenewing(): Boolean {
        return renewMutex.isLocked
    }

    /* 获取 App 的更新状态 */
    fun getReleaseStatus(): Int {
        return updater.getUpdateStatus()
    }

    fun getLatestVersionNumber(): String? {
        return if (isLatestVersion())
            installedVersionNumber ?: appDatabase.ignoreVersionNumber
        else
            versionList.firstOrNull()?.name
    }

    /* 获取 App 的更新状态 */
    fun isLatestVersion(): Boolean {
        return updater.getUpdateStatus() == Updater.APP_LATEST
    }

    private suspend fun renewVersionList(hub: Hub) {
        hub.getAppReleaseList(this)?.let {
            setVersionMap(hub, it)
        }
    }

    private suspend fun setVersionMap(hub: Hub, releaseList: List<ReleaseListItem>) {
        val assetsList = mutableListOf<Asset>()
        releaseList.forEachIndexed { versionIndex, release ->
            val versionNumber = release.versionNumber
            val asset = Asset.newInstance(versionNumber, hub, release.changeLog,
                    release.assetsList.mapIndexed { assetIndex, assetItem ->
                        Asset.Companion.TmpFileAsset(
                                assetItem.fileName,
                                assetItem.downloadUrl,
                                assetItem.fileType,
                                Pair(versionIndex, assetIndex)
                        )
                    }, this)
            assetsList.add(asset)
        }
        versionUtils.addAsset(assetsList, hub.uuid)
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