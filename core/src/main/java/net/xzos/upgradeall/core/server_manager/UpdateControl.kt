package net.xzos.upgradeall.core.server_manager

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.xzos.upgradeall.core.data_manager.utils.DataCache
import net.xzos.upgradeall.core.network_api.GrpcApi
import net.xzos.upgradeall.core.route.AppIdItem
import net.xzos.upgradeall.core.server_manager.module.BaseApp
import net.xzos.upgradeall.core.server_manager.module.app.App
import net.xzos.upgradeall.core.server_manager.module.app.Updater

open class UpdateControl internal constructor(appList: List<BaseApp>) {

    internal var apps: List<BaseApp> = appList

    private val dataMutex = Mutex()  // 保证数据线程安全
    private val refreshMutex = Mutex()  // 刷新锁，避免重复请求刷新导致浪费大量资源
    val appMap: MutableMap<Int, MutableList<BaseApp>> = mutableMapOf()
    private val coroutineDispatcher = Dispatchers.IO

    private suspend fun MutableMap<Int, MutableList<BaseApp>>.addApp(appStatus: Int, app: BaseApp): Boolean {
        return dataMutex.withLock {
            (this@addApp[appStatus] ?: mutableListOf<BaseApp>().also {
                this@addApp[appStatus] = it
            }).add(app)
        }
    }

    private suspend fun MutableMap<Int, MutableList<BaseApp>>.removeApp(app: BaseApp): Boolean {
        var bool = false
        dataMutex.withLock {
            for (list in this.values) {
                if (list.contains(app)) bool = true
                list.remove(app)
            }
        }
        return bool
    }

    private fun setBaseAppUpdateMap(baseApp: BaseApp, updateStatus: Int): Boolean {
        if (appMap[updateStatus]?.contains(baseApp) == true) return false
        runBlocking {
            appMap.removeApp(baseApp)
            appMap.addApp(updateStatus, baseApp)
        }
        return true
    }

    internal open suspend fun updateJob(app: BaseApp) {
        refreshAppUpdate(app)
    }

    suspend fun refreshAppUpdate(baseApp: BaseApp): Pair<Int, Boolean> {
        val updateStatus = baseApp.getUpdateStatus()
        val notify = setBaseAppUpdateMap(baseApp, updateStatus)
        return Pair(updateStatus, notify)
    }

    suspend fun getNeedUpdateAppList(block: Boolean = true): List<BaseApp> {
        return (if (block) refreshMutex.withLock {
            appMap[Updater.APP_OUTDATED]
        } else appMap[Updater.APP_OUTDATED])
                ?: mutableListOf()
    }

    // 刷新所有软件并等待，返回需要更新的软件数量
    suspend fun renewAll(concurrency: Boolean = true, preGetData: Boolean = false) {
        refreshMutex.withLock {
            withContext(coroutineDispatcher) {
                if (preGetData) preGetData(apps)
                // 尝试刷新全部软件
                coroutineScope {
                    for (app in apps) {
                        if (concurrency)
                            launch {
                                updateJob(app)
                            }
                        else updateJob(app)
                    }
                }
            }
        }
    }

    private suspend fun preGetData(appList: List<BaseApp>) {
        val appGroupDict = mutableMapOf<String, MutableList<List<AppIdItem>>>()
        for (app in appList.filterIsInstance<App>()) {
            val hubUuid = app.hubDatabase?.uuid
            val appId = app.appId
            if (hubUuid != null && appId != null
                    && !DataCache.existsAppStatus(hubUuid, appId)) {
                (appGroupDict[hubUuid] ?: mutableListOf<List<AppIdItem>>().also {
                    appGroupDict[hubUuid] = it
                }).add(appId)
            }
        }
        for (hubUuid in appGroupDict.keys)
            GrpcApi.getAppStatusList(hubUuid, appGroupDict[hubUuid]!!)
    }

    internal fun getUpdateStatus(): Int {
        return when {
            runBlocking { getNeedUpdateAppList(block = false) }.isNotEmpty() ->
                Updater.APP_OUTDATED
            appMap[Updater.NETWORK_ERROR]?.size == apps.size ->
                Updater.NETWORK_ERROR
            else -> Updater.APP_LATEST
        }
    }
}
