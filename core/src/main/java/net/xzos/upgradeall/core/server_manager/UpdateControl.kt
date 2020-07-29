package net.xzos.upgradeall.core.server_manager

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.xzos.upgradeall.core.server_manager.module.BaseApp
import net.xzos.upgradeall.core.server_manager.module.app.Updater

open class UpdateControl internal constructor(
        appList: List<BaseApp>,
        internal var appUpdateStatusChangedFun: (app: BaseApp, appStatus: Int) -> Unit
        //  监控 BaseApp 对象更新，用于 Applications 自动记录无效应用 与 UpdateManager 通知更新
) {

    private val dataMutex = Mutex()  // 保证数据线程安全
    internal val refreshMutex = Mutex()  // 刷新锁，避免重复请求刷新导致浪费大量资源
    private val appMap: MutableMap<Int, HashSet<BaseApp>> = mutableMapOf()
    private val coroutineDispatcher = Dispatchers.IO

    fun getAllApp(vararg appStatus: Int): List<BaseApp> {
        val allApp: MutableList<BaseApp> = mutableListOf()
        runAppMapFun {
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
        }
        return allApp
    }

    init {
        addApps(appList.toSet())
    }

    open suspend fun getNeedUpdateAppList(block: Boolean = true): Set<BaseApp> {
        var set: Set<BaseApp> = setOf()
        runAppMapFun {
            val needUpdateAppList = if (block) runBlocking {
                refreshMutex.withLock { appMap[Updater.APP_OUTDATED] }
            } else appMap[Updater.APP_OUTDATED]
            if (!needUpdateAppList.isNullOrEmpty()) {
                set = needUpdateAppList
            }
        }
        return set
    }

    internal fun getAppListFormMap(appStatus: Int): Set<BaseApp> = appMap[appStatus] ?: setOf()

    fun addApp(app: BaseApp) {
        val allApp = getAllApp()
        if (!allApp.contains(app)) {
            addAppInDefList(app)
        }
    }

    fun addApps(appList: Set<BaseApp>) {
        val allApp: HashSet<BaseApp> = hashSetOf()
        for (list in appMap.values)
            allApp.addAll(list)
        for (app in appList) {
            if (!allApp.contains(app)) {
                addAppInDefList(app)
            }
        }
    }

    fun delApp(app: BaseApp) {
        runBlocking { appMap.removeApp(app) }
    }

    fun clearApp() {
        appMap.clear()
    }

    private fun addAppInDefList(app: BaseApp) {
        runBlocking { appMap.addApp(Updater.NETWORK_ERROR, app) }
        app.statusRenewedFun = fun(appStatus: Int) {
            setBaseAppUpdateMap(app, appStatus)
        }
    }

    private fun setBaseAppUpdateMap(baseApp: BaseApp, updateStatus: Int): Boolean {
        if (appMap[updateStatus]?.contains(baseApp) == true) return false
        runBlocking {
            appMap.removeApp(baseApp)
            appMap.addApp(updateStatus, baseApp)
        }
        appUpdateStatusChangedFun(baseApp, updateStatus)
        return true
    }

    // 刷新所有软件并等待，返回需要更新的软件数量
    open suspend fun renewAll() {
        refreshMutex.withLock {
            withContext(coroutineDispatcher) {
                // 尝试刷新全部软件
                coroutineScope {
                    for (app in getAllApp()) {
                        launch {
                            app.getUpdateStatus()
                        }
                    }
                }
            }
        }
    }

    private fun MutableMap<Int, HashSet<BaseApp>>.addApp(appStatus: Int, app: BaseApp) {
        runAppMapFun {
            (this@addApp[appStatus] ?: hashSetOf<BaseApp>().also {
                this@addApp[appStatus] = it
            }).add(app)
        }
    }

    private fun MutableMap<Int, HashSet<BaseApp>>.removeApp(app: BaseApp) {
        runAppMapFun {
            for (list in this.values) {
                list.remove(app)
            }
        }
    }

    private fun runAppMapFun(function: () -> Unit) {
        runBlocking {
            dataMutex.withLock {
                function()
            }
        }
    }
}
