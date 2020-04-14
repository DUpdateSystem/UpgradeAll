package net.xzos.upgradeall.core.server_manager.module.applications

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.xzos.upgradeall.core.data.database.AppDatabase
import net.xzos.upgradeall.core.data.database.getApplicationsAutoExclude
import net.xzos.upgradeall.core.route.AppIdItem
import net.xzos.upgradeall.core.server_manager.UpdateManager
import net.xzos.upgradeall.core.server_manager.module.BaseApp
import net.xzos.upgradeall.core.server_manager.module.app.App
import net.xzos.upgradeall.core.server_manager.module.app.Updater

class Applications(database: AppDatabase) : BaseApp(database) {

    val name = appDatabase.name
    private val applicationsUtils = ApplicationsUtils(appDatabase)

    val apps: MutableList<App> = applicationsUtils.apps
    private val excludeApps: MutableList<App> = applicationsUtils.excludeApps
    private val applicationsUpdateManager = UpdateManager(apps)
    private var initData = false

    // 数据刷新锁
    private val appListMutex = Mutex()

    private fun getAppByAppId(appInfo: List<AppIdItem>, appList: List<App>): App? {
        for (a in appList.filter { it.appId != null }) {
            if (a.appId!![0].value == appInfo[0].value)
                return a
        }
        return null
    }

    private suspend fun refreshAppList(updateManager: UpdateManager): UpdateManager {
        updateManager.renewAll(concurrency = false, preGetData = true)
        val appMap = updateManager.appMap
        excludeInvalidApps(appMap[Updater.INVALID_APP]?.filterIsInstance<App>())
        includeValidApps(appMap[Updater.APP_OUTDATED]?.filterIsInstance<App>())
        includeValidApps(appMap[Updater.APP_LATEST]?.filterIsInstance<App>())
        if (!initData) {
            initData = true
            GlobalScope.launch(Dispatchers.IO) {
                refreshAppList(UpdateManager(excludeApps))
            }
        }
        return updateManager
    }

    suspend fun getNeedUpdateAppList(block: Boolean = true): List<App> {
        return applicationsUpdateManager.getNeedUpdateAppList(block = block).filterIsInstance<App>()
    }

    override suspend fun getUpdateStatus(): Int {
        refreshAppList(applicationsUpdateManager)
        return when {
            getNeedUpdateAppList(block = false).isNotEmpty() ->
                Updater.APP_OUTDATED
            applicationsUpdateManager.appMap[Updater.NETWORK_ERROR]?.size == apps.size ->
                Updater.NETWORK_ERROR
            else -> Updater.APP_LATEST
        }
    }

    private suspend fun includeValidApps(appList: List<App>?) {
        if (appList.isNullOrEmpty()) return
        appListMutex.withLock {
            for (app in appList) {
                if (app in excludeApps) {
                    apps.add(app)
                    excludeApps.remove(app)
                    app.appDatabase.targetChecker?.extraString?.let { packageName ->
                        appDatabase.getApplicationsAutoExclude().remove(packageName)
                    }
                }
            }
            appDatabase.save(false)
        }
    }

    private suspend fun excludeInvalidApps(appList: List<App>?) {
        if (appList.isNullOrEmpty()) return
        appListMutex.withLock {
            for (app in appList) {
                if (app in apps) {
                    apps.remove(app)
                    excludeApps.add(app)
                    app.appDatabase.targetChecker?.extraString?.let { packageName ->
                        appDatabase.getApplicationsAutoExclude().add(packageName)
                    }
                }
            }
            appDatabase.save(false)
        }
    }
}
