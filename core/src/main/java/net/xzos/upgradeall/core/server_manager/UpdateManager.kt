package net.xzos.upgradeall.core.server_manager

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import net.xzos.upgradeall.core.data_manager.utils.DataCache
import net.xzos.upgradeall.core.network_api.GrpcApi
import net.xzos.upgradeall.core.route.AppIdItem
import net.xzos.upgradeall.core.server_manager.module.BaseApp
import net.xzos.upgradeall.core.server_manager.module.app.App
import net.xzos.upgradeall.core.server_manager.module.app.Updater
import net.xzos.upgradeall.core.system_api.RegisterApi
import net.xzos.upgradeall.core.system_api.annotations.UpdateManagerApi


private val updateFinishedAnnotation =
        UpdateManagerApi.statusRefresh::class.java

class UpdateManager internal constructor(
        appsList: List<BaseApp>? = null
) : RegisterApi(
        updateFinishedAnnotation
) {
    val apps: List<BaseApp> = appsList ?: AppManager.apps
    var finishedAppNum: Long = 0
    private val dataMutex = Mutex()  // 保证数据线程安全
    private val refreshMutex = Mutex()  // 刷新锁，避免重复请求刷新导致浪费大量资源
    val appMap: MutableMap<Int, MutableList<BaseApp>> = mutableMapOf()
    private val coroutineDispatcher = Dispatchers.IO

    private fun MutableMap<Int, MutableList<BaseApp>>.addApp(appStatus: Int, app: BaseApp): Boolean {
        return (this@addApp[appStatus] ?: mutableListOf<BaseApp>().also {
            this@addApp[appStatus] = it
        }).add(app)
    }

    private fun resetVariable() {
        appMap.clear()
        finishedAppNum = 0
    }

    private suspend fun updateJob(app: BaseApp) {
        val updateStatus = app.getUpdateStatus()
        dataMutex.withLock {
            appMap.addApp(updateStatus, app)
            finishedAppNum++
        }
        notifyChange()
    }

    suspend fun getNeedUpdateAppList(block: Boolean = true): List<BaseApp> {
        return (if (block) refreshMutex.withLock {
            appMap[Updater.APP_OUTDATED]
        } else appMap[Updater.APP_OUTDATED])
                ?: mutableListOf()
    }

    private fun notifyChange() {
        if (this == updateManager) {
            runNoReturnFun(updateFinishedAnnotation)
        }
    }

    // 刷新所有软件并等待，返回需要更新的软件数量
    suspend fun renewAll(concurrency: Boolean = true, preGetData: Boolean = false) {
        refreshMutex.withLock {
            withContext(coroutineDispatcher) {
                if (preGetData) preGetData(apps)
                resetVariable()
                // 尝试刷新全部软件
                coroutineScope {
                    for (app in apps) {
                        if (concurrency)
                            launch {
                                updateJob(app)
                            }
                        else updateJob(app)
                    }
                    notifyChange()  // 初始化更新应用通知
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

    companion object {
        val updateManager = UpdateManager()
    }
}
