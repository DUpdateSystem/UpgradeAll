package net.xzos.upgradeall.core.server_manager.module.applications

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.xzos.upgradeall.core.data.database.ApplicationsDatabase
import net.xzos.upgradeall.core.data_manager.AppDatabaseManager
import net.xzos.upgradeall.core.network_api.GrpcReleaseApi
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
        get() = runBlocking {
            updateControl.getNeedUpdateAppList(false)
        }.filterIsInstance<App>()


    private var tmpUpdateStatus = 0

    private val applicationsUtils = ApplicationsUtils(appDatabase)
    private val updateControl = UpdateControl(applicationsUtils.getApps(), fun(app, appStatus) {
        if (appStatus == Updater.INVALID_APP && app is App && app.appId != null) {
            runBlocking {
                markInvalidApp(app)
            }
        }
        notifyChanged()  // 通知应用列表改变
        checkUpdateStatusChanged()
    })

    private var checkAppList = true

    val appList: List<App>
        get() = updateControl.getAllApp(Updater.APP_OUTDATED, Updater.APP_LATEST, Updater.NETWORK_ERROR).filterIsInstance<App>()

    override suspend fun getUpdateStatus(): Int {
        updateControl.renewAll()
        if (checkAppList) {
            checkAppList = false
            GlobalScope.launch {
                renewExcludeApp()
            }
        }
        return nonBlockGetUpdateStatus()
    }

    private suspend fun renewExcludeApp() {
        val hubUuid = appDatabase.hubUuid
        val auth = appDatabase.auth
        val excludeAppIdList = applicationsUtils.getExcludeAppIdList()
        for ((appId, appInfo) in excludeAppIdList)
            GrpcReleaseApi.setRequest(hubUuid, auth, appId, fun(releaseList) {
                if (releaseList != null) {
                    if (releaseList.isNotEmpty()) {
                        runBlocking { markValidApp(appId) }
                        updateControl.addApp(applicationsUtils.mkApp(appInfo))
                    }
                }
            })
        GrpcReleaseApi.chunkedCallGetAppRelease(hubUuid, auth, excludeAppIdList.keys, 2)
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
        runBlocking { updateControl.getNeedUpdateAppList(false) }.isNotEmpty() ->
            Updater.APP_OUTDATED
        updateControl.getAppListFormMap(Updater.NETWORK_ERROR).size == updateControl.getAllApp().size ->
            Updater.NETWORK_ERROR
        else -> Updater.APP_LATEST
    }

    private suspend fun markValidApp(appId: Map<String, String?>) {
        appDatabase.invalidPackageList.remove(appId)
        AppDatabaseManager.updateApplicationsDatabase(appDatabase)
    }

    private suspend fun markInvalidApp(app: App) {
        updateControl.delApp(app)
        markInvalidApp(app.appId!!)
    }

    private suspend fun markInvalidApp(appId: Map<String, String?>) {
        appDatabase.invalidPackageList.add(appId)
        AppDatabaseManager.updateApplicationsDatabase(appDatabase)
    }
}
