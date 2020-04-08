package net.xzos.upgradeall.core.server_manager

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.xzos.upgradeall.core.server_manager.module.BaseApp
import net.xzos.upgradeall.core.server_manager.module.app.Updater
import net.xzos.upgradeall.core.system_api.RegisterApi
import net.xzos.upgradeall.core.system_api.annotations.UpdateManagerApi
import java.util.concurrent.Executors


private val updateFinishedAnnotation =
        UpdateManagerApi.statusRefresh::class.java

class UpdateManager internal constructor(
        appsList: List<BaseApp>? = null
) : RegisterApi(
        updateFinishedAnnotation
) {
    val apps: List<BaseApp> = appsList ?: AppManager.apps
    var finishedAppNum: Long = 0
    private val dataMutex = Mutex()  // 保证线程安全
    private val refreshMutex = Mutex()  // 刷新锁，避免重复请求刷新导致浪费大量资源
    val appMap: MutableMap<Int, MutableList<BaseApp>> = mutableMapOf()
    private val coroutineDispatcher =
            if (this == updateManager) Dispatchers.IO
            else Executors.newSingleThreadExecutor().asCoroutineDispatcher()

    private fun MutableMap<Int, MutableList<BaseApp>>.addApp(appStatus: Int, app: BaseApp): Boolean {
        return runBlocking {
            dataMutex.withLock {
                (this@addApp[appStatus] ?: mutableListOf<BaseApp>().also {
                    this@addApp[appStatus] = it
                }).add(app)
            }
        }
    }

    private fun resetVariable() {
        appMap.clear()
        finishedAppNum = 0
    }

    private suspend fun updateJob(app: BaseApp) {
        appMap.addApp(app.getUpdateStatus(), app)
        dataMutex.withLock {
            finishedAppNum++
            notifyChange()
        }
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
    suspend fun renewAll(concurrency: Boolean = true) {
        refreshMutex.withLock {
            resetVariable()
            // 尝试刷新全部软件
            coroutineScope {
                for (app in apps) {
                    if (concurrency) {
                        launch(coroutineDispatcher) {
                            updateJob(app)
                        }
                    } else withContext(coroutineDispatcher) {
                        updateJob(app)
                    }
                }
                notifyChange()  // 初始化更新应用通知
            }
        }
    }

    companion object {
        val updateManager = UpdateManager()
    }
}
