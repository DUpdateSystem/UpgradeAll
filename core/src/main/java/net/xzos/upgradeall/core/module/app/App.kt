package net.xzos.upgradeall.core.module.app

import kotlinx.coroutines.runBlocking
import net.xzos.upgradeall.core.database.table.AppEntity
import net.xzos.upgradeall.core.database.table.isInit
import net.xzos.upgradeall.core.database.table.setSortHubUuidList
import net.xzos.upgradeall.core.manager.HubManager
import net.xzos.upgradeall.core.module.Hub
import net.xzos.upgradeall.core.module.app.data.DataStorage
import net.xzos.upgradeall.core.module.app.version.Version
import net.xzos.upgradeall.core.module.app.version.VersionEntityUtils
import net.xzos.upgradeall.core.module.app.version.VersionInfo
import net.xzos.upgradeall.core.websdk.api.ServerApiProxy
import net.xzos.upgradeall.core.websdk.json.AppConfigGson

@Suppress("DataClassPrivateConstructor")
data class App private constructor(val appDatabase: AppEntity) {
    private val dataStorage = DataStorage(appDatabase)
    val serverApi: ServerApiProxy = dataStorage.serverApi
    val entityUtils = VersionEntityUtils(dataStorage.appDatabase)
    private val updater = Updater(dataStorage, entityUtils)

    /* App 对象的属性字典 */
    val appId: Map<String, String?> get() = appDatabase.appId

    /* App 名称 */
    val name get() = appDatabase.name

    /* 这个 App 已启用的软件源 */
    val hubEnableList: List<Hub>
        get() = dataStorage.hubList

    /* 是否星标 */
    val star get() = appDatabase.star

    suspend fun setHubList(hubUuidList: List<String>) {
        dataStorage.appDatabase.setSortHubUuidList(hubUuidList)
    }

    val isActive: Boolean
        get() {
            hubEnableList.forEach {
                if (it.isActiveApp(appId))
                    return true
            }
            return false
        }

    val isVirtual: Boolean
        get() = !appDatabase.isInit()

    /* App 在本地的版本号 */
    var localVersion = updater.getLocalVersion()


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
    val versionList: List<Version> get() = runBlocking { dataStorage.versionMap.getVersionList() }

    /* 刷新版本号数据 */
    suspend fun update() {
        updater.update()
    }

    suspend fun getReleaseStatusWaitRenew(): Int {
        return updater.getReleaseStatusWaitRenew()
    }

    fun isRenewing(): Boolean {
        return updater.isRenewing()
    }

    /* 获取 App 的更新状态 */
    fun getReleaseStatus(): Int {
        return updater.getReleaseStatus()
    }

    fun getLatestVersion(): VersionInfo? {
        return if (isLatestVersion())
            localVersion ?: appDatabase.getIgnoreVersion()
        else versionList.firstOrNull()?.versionInfo
    }

    /* 获取 App 的更新状态 */
    fun isLatestVersion(): Boolean {
        return updater.getReleaseStatus() == Updater.APP_LATEST
    }

    companion object {
        fun new(
            appDatabase: AppEntity,
            statusRenewedFun: (appStatus: Int) -> Unit = fun(_: Int) {},
        ) = App(appDatabase).apply {
            setStatusRenewedFun(statusRenewedFun)
        }
    }
}

fun App.getDatabase(): AppEntity = appDatabase
fun App.getConfigJson(): AppConfigGson? = appDatabase.cloudConfig

/* 这个 App 数据可用的软件源 */
val App.hubAvailableList: List<Hub>
    get() = versionList.flatMap { version -> version.versionList.map { it.hub } }