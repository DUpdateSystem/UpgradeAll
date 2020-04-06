package net.xzos.upgradeall.core.server_manager.module.applications

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.xzos.upgradeall.core.data.database.AppDatabase
import net.xzos.upgradeall.core.data.database.getApplicationsAutoExclude
import net.xzos.upgradeall.core.route.AppInfoItem
import net.xzos.upgradeall.core.server_manager.UpdateManager
import net.xzos.upgradeall.core.server_manager.module.BaseApp
import net.xzos.upgradeall.core.server_manager.module.app.App
import net.xzos.upgradeall.core.server_manager.module.app.Updater

class Applications(database: AppDatabase) : BaseApp(database) {

    val name = appDatabase.name
    private val applicationsUtils = ApplicationsUtils(appDatabase)

    val apps: MutableList<App> = applicationsUtils.apps
    private val excludeApps: MutableList<App> = applicationsUtils.excludeApps
    private var updateManager = UpdateManager(apps)

    // 数据刷新锁
    private val dataMutex = Mutex()
    private val appListMutex = Mutex()

    init {
        GlobalScope.launch {
            refreshAppList(excludeApps)
        }
    }

    private fun getAppByAppInfo(appInfo: List<AppInfoItem>, appList: List<App>): App? {
        for (a in appList.filter { it.appInfo != null }) {
            if (a.appInfo!![0].value == appInfo[0].value)
                return a
        }
        return null
    }

    private suspend fun refreshAppList(appList: List<App>): UpdateManager {
        val appsUpdateManager = UpdateManager(appList)
        return appsUpdateManager.also {
            it.renewAll()
            val appMap = it.appMap
            excludeInvalidApps(appMap[Updater.NETWORK_404]?.filterIsInstance<App>())
            includeValidApps(appMap[Updater.APP_OUTDATED]?.filterIsInstance<App>())
            includeValidApps(appMap[Updater.APP_LATEST]?.filterIsInstance<App>())
        }
    }

    suspend fun getNeedUpdateAppList(block: Boolean = true): List<App> {
        val list = if (block) dataMutex.withLock {
            updateManager.needUpdateAppList
        }
        else updateManager.appMap[Updater.APP_OUTDATED]
        return list?.filterIsInstance<App>() ?: listOf()
    }

    override suspend fun getUpdateStatus(): Int {
        dataMutex.withLock {
            updateManager = refreshAppList(apps)
            return when {
                updateManager.needUpdateAppList.isNotEmpty() ->
                    Updater.APP_OUTDATED
                updateManager.appMap[Updater.NETWORK_ERROR]?.size == apps.size ->
                    Updater.NETWORK_ERROR
                else -> Updater.APP_LATEST
            }
        }
    }

    private suspend fun includeValidApps(appList: List<App>?) {
        if (appList.isNullOrEmpty()) return
        appListMutex.withLock {
            for (app in appList) {
                if (app in apps) {
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
