package net.xzos.upgradeall.core.module.app

import net.xzos.upgradeall.core.database.table.AppEntity
import net.xzos.upgradeall.core.database.table.isInit
import net.xzos.upgradeall.core.database.table.setSortHubUuidList
import net.xzos.upgradeall.core.manager.HubManager
import net.xzos.upgradeall.core.module.Hub
import net.xzos.upgradeall.core.module.app.data.DataStorage
import net.xzos.upgradeall.core.module.app.version.Version
import net.xzos.upgradeall.core.websdk.ServerApiProxy
import net.xzos.upgradeall.core.websdk.json.AppConfigGson

class App(
    appDatabase: AppEntity,
    statusRenewedFun: (appStatus: Int) -> Unit = fun(_: Int) {},
) {
    private val dataStorage = DataStorage(appDatabase)
    internal val appDatabase: AppEntity = dataStorage.appDatabase
    val serverApi: ServerApiProxy = dataStorage.serverApi
    private val updater = Updater(dataStorage, statusRenewedFun)

    /* App 对象的属性字典 */
    val appId: Map<String, String> get() = appDatabase.appId

    /* App 名称 */
    val name get() = appDatabase.name

    /* 这个 App 可用的软件源 */
    val hubList: List<Hub>
        get() = dataStorage.hubList

    /* 是否星标 */
    val star get() = appDatabase.star

    suspend fun setHubList(hubUuidList: List<String>) {
        dataStorage.appDatabase.setSortHubUuidList(hubUuidList)
    }

    val isActive: Boolean
        get() {
            hubList.forEach {
                if (it.isActiveApp(appId))
                    return true
            }
            return false
        }

    val isVirtual: Boolean
        get() = !appDatabase.isInit()

    /* App 在本地的版本号 */
    val rawInstalledVersionStringList: List<Pair<Char, Boolean>>? =
        updater.getRawInstalledVersionStringList()

    val installedVersionNumber: String? = updater.getInstalledVersionNumber()


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
    val versionList: List<Version> get() = dataStorage.versionData.getVersionList()

    /* 刷新版本号数据 */
    suspend fun update() {
        updater.update()
    }

    suspend fun getReleaseStatusWaitRenew(): Int {
        return updater.getReleaseStatusWaitRenew()
    }

    fun isRenewing(): Boolean {
        return dataStorage.versionData.isLocked()
    }

    /* 获取 App 的更新状态 */
    fun getReleaseStatus(): Int {
        return updater.getReleaseStatus()
    }

    fun getLatestVersionNumber(): String? {
        return if (isLatestVersion())
            installedVersionNumber ?: appDatabase.ignoreVersionNumber
        else
            versionList.firstOrNull()?.name
    }

    /* 获取 App 的更新状态 */
    fun isLatestVersion(): Boolean {
        return updater.getReleaseStatus() == Updater.APP_LATEST
    }

    override fun equals(other: Any?): Boolean {
        return when (other) {
            !is App -> false
            else -> hashCode() == other.hashCode()
        }
    }

    override fun hashCode(): Int {
        return dataStorage.hashCode()
    }
}

fun App.getDatabase(): AppEntity = appDatabase
fun App.getConfigJson(): AppConfigGson? = appDatabase.cloudConfig