package net.xzos.upgradeall.core.server_manager

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.xzos.upgradeall.core.data.coroutines.CoroutinesMutableList
import net.xzos.upgradeall.core.data.coroutines.CoroutinesMutableMap
import net.xzos.upgradeall.core.data.coroutines.coroutinesMutableListOf
import net.xzos.upgradeall.core.data.coroutines.coroutinesMutableMapOf
import net.xzos.upgradeall.core.server_manager.module.BaseApp
import net.xzos.upgradeall.core.server_manager.module.app.Updater

open class UpdateControl internal constructor(
        appList: List<BaseApp>,
        internal var appUpdateStatusChangedFun: (app: BaseApp, appStatus: Int) -> Unit
        //  监控 BaseApp 对象更新，用于 Applications 自动记录无效应用 与 UpdateManager 通知更新
) {

    internal val refreshMutex = Mutex()  // 刷新锁，避免重复请求刷新导致浪费大量资源
    private val appMap = coroutinesMutableMapOf<Int, CoroutinesMutableList<BaseApp>>()

    fun getAllApp(vararg appStatus: Int): List<BaseApp> {
        val allApp: MutableList<BaseApp> = mutableListOf()
        if (appStatus.isNotEmpty()) {
            for (i in appStatus) {
                appMap[i]?.let {
                    allApp.addAll(it)
                }
            }
        } else {
            for (list in appMap.values)
                allApp.addAll(list)
        }
        return allApp
    }

    init {
        addApps(appList.toSet())
    }

    open suspend fun getNeedUpdateAppList(block: Boolean = true): Set<BaseApp> {
        var set: Set<BaseApp> = setOf()
        val needUpdateAppList = if (block)
            refreshMutex.withLock { appMap[Updater.APP_OUTDATED] }
        else appMap[Updater.APP_OUTDATED]
        if (!needUpdateAppList.isNullOrEmpty()) {
            set = needUpdateAppList.toSet()
        }
        return set
    }

    internal fun getAppListFormMap(appStatus: Int): Set<BaseApp> = appMap[appStatus]?.toSet()
            ?: setOf()

    fun addApp(app: BaseApp) {
        val allApp = getAllApp()
        if (!allApp.contains(app)) {
            addAppInDefList(app)
        }
    }

    fun addApps(appList: Set<BaseApp>) {
        val allApp = getAllApp()
        for (app in appList) {
            if (!allApp.contains(app)) {
                addAppInDefList(app)
            }
        }
    }

    fun delApp(app: BaseApp) {
        appMap.removeApp(app)
    }

    fun clearApp() {
        appMap.clear()
    }

    private fun addAppInDefList(app: BaseApp) {
        appMap.addApp(Updater.NETWORK_ERROR, app)
        app.statusRenewedFun = fun(appStatus: Int) {
            setBaseAppUpdateMap(app, appStatus)
        }
    }

    private fun setBaseAppUpdateMap(baseApp: BaseApp, updateStatus: Int): Boolean {
        if (appMap[updateStatus]?.contains(baseApp) == true) return false
        delApp(baseApp)
        appMap.addApp(updateStatus, baseApp)
        appUpdateStatusChangedFun(baseApp, updateStatus)
        return true
    }

    // 刷新所有软件并等待，返回需要更新的软件数量
    open suspend fun renewAll() {
        refreshMutex.withLock {
            coroutineScope {
                // 尝试刷新全部软件
                for (app in getAllApp()) {
                    launch {
                        app.getUpdateStatus()
                    }
                }
            }
        }
    }

    private fun CoroutinesMutableMap<Int, CoroutinesMutableList<BaseApp>>.addApp(appStatus: Int, app: BaseApp) {
        (this@addApp[appStatus] ?: coroutinesMutableListOf<BaseApp>(true).also {
            this@addApp[appStatus] = it
        }).add(app)
    }

    private fun CoroutinesMutableMap<Int, CoroutinesMutableList<BaseApp>>.removeApp(app: BaseApp) {
        for (list in this.values) {
            list.remove(app)
        }
    }
}
