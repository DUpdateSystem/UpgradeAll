package net.xzos.upgradeall.core.server_manager

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.xzos.upgradeall.core.server_manager.module.BaseApp
import net.xzos.upgradeall.core.server_manager.module.app.Updater
import net.xzos.upgradeall.core.system_api.RegisterApi
import net.xzos.upgradeall.core.system_api.annotations.UpdateManagerApi


private val updateFinishedAnnotation =
    UpdateManagerApi.statusRefresh::class.java

class UpdateManager internal constructor(
    private val appsList: List<BaseApp>? = null
) : RegisterApi(
    updateFinishedAnnotation
) {
    val apps: List<BaseApp>
        get() = appsList ?: AppManager.apps
    var finishedAppNum: Long = 0
    private val dataMutex = Mutex()  // 保证线程安全
    private val refreshMutex = Mutex()  // 刷新锁，避免重复请求刷新导致浪费大量资源
    val appMap: MutableMap<Int, MutableList<BaseApp>> = mutableMapOf()
    val needUpdateAppList: List<BaseApp>
        get() =
            runBlocking {
                refreshMutex.withLock {
                    appMap[Updater.APP_OUTDATED] ?: listOf<BaseApp>()
                }
            }

    private fun MutableMap<Int, MutableList<BaseApp>>.addApp(appStatus: Int, app: BaseApp): Boolean {
        return runBlocking {
            dataMutex.withLock {
                (this@addApp[appStatus] ?: mutableListOf<BaseApp>().also {
                    this@addApp[appStatus] = it
                }).add(app)
            }
        }
    }

    private fun notifyChange() {
        GlobalScope.launch {
            runNoReturnFun(updateFinishedAnnotation)
        }
    }

    // 刷新所有软件并等待，返回需要更新的软件数量
    suspend fun renewAll(concurrency: Boolean = true) {
        refreshMutex.withLock {
            // 尝试刷新全部软件
            coroutineScope {
                for (app in apps) {
                    if (concurrency) {
                        launch(Dispatchers.IO) {
                            updateJob(app)
                        }
                    } else withContext(Dispatchers.IO) {
                        updateJob(app)
                    }
                }
                notifyChange()  // 初始化更新应用通知
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

    companion object {
        val updateManager = UpdateManager()
    }
}
