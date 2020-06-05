package net.xzos.upgradeall.core.server_manager

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.xzos.upgradeall.core.oberver.Informer
import net.xzos.upgradeall.core.oberver.Observer
import net.xzos.upgradeall.core.server_manager.module.AppHub
import net.xzos.upgradeall.core.server_manager.module.BaseApp
import net.xzos.upgradeall.core.server_manager.module.app.App
import net.xzos.upgradeall.core.server_manager.module.app.Updater
import net.xzos.upgradeall.core.server_manager.module.app.getParentApplications
import net.xzos.upgradeall.core.server_manager.module.applications.Applications


object UpdateManager : UpdateControl(AppManager.apps), AppHub, Informer {
    var finishedUpdateAppNum: Long = 0

    init {
        AppManager.observeForever(
                object : Observer {
                    override fun onChanged(vars: Array<out Any>): Any? {
                        apps = AppManager.apps
                        return null
                    }
                }
        )
    }

    fun getAppNum(): Int = apps.size
    override suspend fun getAppUpdateStatus(baseApp: BaseApp): Int {
        if (baseApp is App) {
            val applications = baseApp.getParentApplications()
            if (applications != null)
                return applications.getAppUpdateStatus(baseApp)
        }
        return refreshAppUpdate(baseApp).also {
            if (it.second) {
                notifyChanged()
            }
        }.first
    }

    suspend fun renewAll() {
        finishedUpdateAppNum = 0
        renewAll(concurrency = true, preGetData = false)
        notifyChanged()
    }

    override suspend fun updateJob(app: BaseApp) {
        super.updateJob(app)
        finishedUpdateAppNum++
        notifyChanged()
    }

    suspend fun downloadAllUpdate() {
        val appList = mutableListOf<App>()
        for (app in getNeedUpdateAppList(block = false)) {
            when (app) {
                is App -> appList.add(app)
                is Applications -> appList.addAll(app.getNeedUpdateAppList(block = false))
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
