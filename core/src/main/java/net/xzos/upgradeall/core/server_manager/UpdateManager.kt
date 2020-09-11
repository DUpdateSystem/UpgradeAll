package net.xzos.upgradeall.core.server_manager

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.xzos.upgradeall.core.data.coroutines_basic_data_type.coroutinesMutableListOf
import net.xzos.upgradeall.core.oberver.Informer
import net.xzos.upgradeall.core.server_manager.module.BaseApp
import net.xzos.upgradeall.core.server_manager.module.app.App
import net.xzos.upgradeall.core.server_manager.module.app.Updater
import net.xzos.upgradeall.core.server_manager.module.applications.Applications


object UpdateManager : UpdateControl(AppManager.apps, fun(_, _) {}), Informer {
    const val UPDATE_STATUS_CHANGED = "UPDATE_RUNNING"
    val finishedUpdateAppNum: Int get() = finishedUpdateApp.size

    private val finishedUpdateApp = coroutinesMutableListOf<BaseApp>()

    val isRunning: Boolean get() = refreshMutex.isLocked

    init {
        appUpdateStatusChangedFun = fun(baseApp, _) {
            finishedUpdateApp.add(baseApp)
            notifyChanged(UPDATE_STATUS_CHANGED)
        }
        AppManager.observeForever<Unit>(fun(_) {
            clearApp()
            addApps(AppManager.apps.toSet())
        })
    }

    fun getAppNum(): Int = getAllApp().size

    override suspend fun renewAll() {
        finishedUpdateApp.clear()
        super.renewAll()
    }

    suspend fun downloadAllUpdate() {
        val appList = mutableListOf<App>()
        for (baseApp in getNeedUpdateAppList(block = false)) {
            when (baseApp) {
                is App -> appList.add(baseApp)
                is Applications -> appList.addAll(baseApp.needUpdateAppList)
            }
        }
        withContext(Dispatchers.IO) {
            for (app in appList) {
                launch(Dispatchers.IO) {
                    Updater(app).downloadReleaseFile(Pair(0, 0))
                }
            }
        }
    }
}
