package net.xzos.upgradeall.core.server_manager

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.xzos.upgradeall.core.oberver.Informer
import net.xzos.upgradeall.core.oberver.Observer
import net.xzos.upgradeall.core.server_manager.module.BaseApp
import net.xzos.upgradeall.core.server_manager.module.app.App
import net.xzos.upgradeall.core.server_manager.module.app.Updater
import net.xzos.upgradeall.core.server_manager.module.applications.Applications


object UpdateManager : UpdateControl(AppManager.apps, fun(_, _) {}), Informer {
    const val UPDATE_RUNNING = "UPDATE_RUNNING"
    const val UPDATE_FINISHED = "UPDATE_FINISHED"
    val finishedUpdateAppNum: Int get() = finishedUpdateApp.size

    private val finishedUpdateApp: HashSet<BaseApp> = hashSetOf()

    val isRunning: Boolean get() = refreshMutex.isLocked

    init {
        appUpdateStatusChangedFun = fun(baseApp, _) {
            finishedUpdateApp.add(baseApp)
            notifyChanged(UPDATE_RUNNING)
        }
        AppManager.observeForever(
                object : Observer {
                    override fun onChanged(vars: Array<out Any>): Any? {
                        clearApp()
                        addApps(AppManager.apps.toSet())
                        return null
                    }
                }
        )
    }

    fun getAppNum(): Int = getAllApp().size

    override suspend fun renewAll() {
        finishedUpdateApp.clear()
        super.renewAll()
        notifyChanged(UPDATE_FINISHED)
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
