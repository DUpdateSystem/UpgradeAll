package net.xzos.upgradeall.core.server_manager.module.applications

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.xzos.upgradeall.core.data.database.AppDatabase
import net.xzos.upgradeall.core.oberver.Informer
import net.xzos.upgradeall.core.server_manager.UpdateControl
import net.xzos.upgradeall.core.server_manager.module.BaseApp
import net.xzos.upgradeall.core.server_manager.module.app.App
import net.xzos.upgradeall.core.server_manager.module.app.Updater

class Applications(override val appDatabase: AppDatabase,
                   override var statusRenewedFun: (appStatus: Int) -> Unit = fun(_) {}
) : BaseApp, Informer {

    val name = appDatabase.name
    private val markAppMutex = Mutex()

    val needUpdateAppList: List<App>
        get() = runBlocking { mainUpdateControl.getNeedUpdateAppList(false) }.filterIsInstance<App>()


    private var tmpUpdateStatus = 0

    private val applicationsUtils = ApplicationsUtils(appDatabase)
    private val mainUpdateControl = UpdateControl(applicationsUtils.apps, fun(app, appStatus) {
        if (appStatus == Updater.INVALID_APP && app is App) {
            markInvalidApp(app)
        }
        notifyChanged()  // 通知应用列表改变
        checkUpdateStatusChanged()
    })
    private val otherUpdateControl = UpdateControl(applicationsUtils.excludeApps, fun(app, appStatus) {
        if ((appStatus == Updater.APP_OUTDATED || appStatus == Updater.APP_LATEST) && app is App) {
            markValidApp(app)
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

    private fun markValidApp(app: App) {
        mainUpdateControl.addApp(app)
        otherUpdateControl.delApp(app)
        val packageName = app.appId?.get(0)?.value
        runMarkAppFun {
            appDatabase.extraData?.applicationsConfig?.invalidPackageName?.remove(packageName)
            appDatabase.save(false)
        }
    }

    private fun markInvalidApp(app: App) {
        mainUpdateControl.delApp(app)
        otherUpdateControl.addApp(app)
        val packageName = app.appId?.get(0)?.value ?: return
        runMarkAppFun {
            appDatabase.extraData?.applicationsConfig?.invalidPackageName?.add(packageName)
            appDatabase.save(false)
        }
    }

    private fun runMarkAppFun(function: () -> Unit) {
        runBlocking { markAppMutex.withLock { function() } }
    }
}
