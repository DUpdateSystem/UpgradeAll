package net.xzos.upgradeall.core.server_manager

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.xzos.upgradeall.core.server_manager.module.app.App
import net.xzos.upgradeall.core.server_manager.module.app.Updater
import net.xzos.upgradeall.core.server_manager.module.applications.Applications
import net.xzos.upgradeall.core.system_api.annotations.UpdateManagerApi


private val updateFinishedAnnotation =
        UpdateManagerApi.statusRefresh::class.java

object UpdateManager : UpdateControl(AppManager.apps, updateFinishedAnnotation) {
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
