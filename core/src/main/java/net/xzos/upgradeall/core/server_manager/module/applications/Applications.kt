package net.xzos.upgradeall.core.server_manager.module.applications

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.xzos.upgradeall.core.data.database.ApplicationsDatabase
import net.xzos.upgradeall.core.data_manager.AppDatabaseManager
import net.xzos.upgradeall.core.oberver.Informer
import net.xzos.upgradeall.core.server_manager.UpdateControl
import net.xzos.upgradeall.core.server_manager.module.BaseApp
import net.xzos.upgradeall.core.server_manager.module.app.App
import net.xzos.upgradeall.core.server_manager.module.app.Updater

class Applications(override val appDatabase: ApplicationsDatabase,
                   override var statusRenewedFun: (appStatus: Int) -> Unit = fun(_) {}
) : BaseApp, Informer {

    val name = appDatabase.name

    val needUpdateAppList: List<App>
        get() = runBlocking { mainUpdateControl.getNeedUpdateAppList(false) }.filterIsInstance<App>()


    private var tmpUpdateStatus = 0

    private val applicationsUtils = ApplicationsUtils(appDatabase)
    private val mainUpdateControl = UpdateControl(applicationsUtils.apps, fun(app, appStatus) {
        if (appStatus == Updater.INVALID_APP && app is App) {
            runBlocking {
                markInvalidApp(app)
            }
        }
        notifyChanged()  // 通知应用列表改变
        checkUpdateStatusChanged()
    })
    private val otherUpdateControl = UpdateControl(applicationsUtils.excludeApps, fun(app, appStatus) {
        if ((appStatus == Updater.APP_OUTDATED || appStatus == Updater.APP_LATEST) && app is App) {
            runBlocking { markValidApp(app) }
            notifyChanged()
        }
        checkUpdateStatusChanged()
    })

    private var checkAppList = true

    val appList: List<App>
        get() = mainUpdateControl.getAllApp(Updater.APP_OUTDATED, Updater.APP_LATEST, Updater.NETWORK_ERROR).filterIsInstance<App>()

    override suspend fun getUpdateStatus(): Int {
        mainUpdateControl.renewAll()
        if (checkAppList) {
            checkAppList = false
            GlobalScope.launch {
                otherUpdateControl.renewAll()
            }
        }
        return nonBlockGetUpdateStatus()
    }

    override fun refreshData() {
    }

    private fun checkUpdateStatusChanged() {
        val updateStatus = nonBlockGetUpdateStatus()
        if (updateStatus != tmpUpdateStatus) {
            tmpUpdateStatus = updateStatus
            statusRenewedFun(updateStatus)
        }
    }

    private fun nonBlockGetUpdateStatus(): Int = when {
        runBlocking { mainUpdateControl.getNeedUpdateAppList(false) }.isNotEmpty() ->
            Updater.APP_OUTDATED
        mainUpdateControl.getAppListFormMap(Updater.NETWORK_ERROR).size == mainUpdateControl.getAllApp().size ->
            Updater.NETWORK_ERROR
        else -> Updater.APP_LATEST
    }

    private suspend fun markValidApp(app: App) {
        mainUpdateControl.addApp(app)
        otherUpdateControl.delApp(app)
        val appId = app.appId ?: return
        appDatabase.invalidPackageList.remove(appId)
        AppDatabaseManager.updateApplicationsDatabase(appDatabase)
    }

    private suspend fun markInvalidApp(app: App) {
        mainUpdateControl.delApp(app)
        otherUpdateControl.addApp(app)
        val appId = app.appId ?: return
        appDatabase.invalidPackageList.add(appId)
        AppDatabaseManager.updateApplicationsDatabase(appDatabase)
    }
}
