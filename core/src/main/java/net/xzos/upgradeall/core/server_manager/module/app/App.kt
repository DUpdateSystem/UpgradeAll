package net.xzos.upgradeall.core.server_manager.module.app

import kotlinx.coroutines.runBlocking
import net.xzos.upgradeall.core.data.database.AppDatabase
import net.xzos.upgradeall.core.data.json.gson.IgnoreApp
import net.xzos.upgradeall.core.data_manager.AppDatabaseManager
import net.xzos.upgradeall.core.data_manager.HubDatabaseManager
import net.xzos.upgradeall.core.data_manager.utils.AutoTemplate
import net.xzos.upgradeall.core.data_manager.utils.VersioningUtils.FOREVER_IGNORE
import net.xzos.upgradeall.core.server_manager.AppManager
import net.xzos.upgradeall.core.server_manager.module.BaseApp
import net.xzos.upgradeall.core.server_manager.module.applications.Applications
import net.xzos.upgradeall.core.system_api.api.IoApi

class App(override val appDatabase: AppDatabase, appId: Map<String, String>? = null) : BaseApp {
    override var statusRenewedFun: (appStatus: Int) -> Unit = fun(_: Int) {}
    val hubDatabase = HubDatabaseManager.getDatabase(appDatabase.hubUuid)
    var appId: Map<String, String>? = appId ?: getAppIdByUrl()

    private fun getAppIdByUrl(): Map<String, String>? {
        if (hubDatabase != null) {
            if (hubDatabase.hubConfig.apiKeywords.isEmpty())
                return mapOf()
            for (appUrlTemplate in hubDatabase.hubConfig.appUrlTemplates) {
                return AutoTemplate(appDatabase.url, appUrlTemplate).args.mapKeys {
                    it.key.substringAfterLast("%")
                }.filter { it.value.isNotBlank() } + appDatabase.extraId
            }
        }
        return null
    }

    var ignoreVersionNumber: String?
        get() {
            val appId = this.appId ?: return null
            return getParentApplications()?.appDatabase?.getIgnoreVersionNumber(appId)
                    ?: appDatabase.ignoreVersionNumber
        }
        set(value) {
            val appId = this.appId ?: return
            getParentApplications()?.appDatabase?.ignoreApps?.add(IgnoreApp.getInstance(appId, value))
                    ?: kotlin.run { appDatabase.ignoreVersionNumber = value }
        }

    override suspend fun getUpdateStatus(): Int {
        return Updater(this).getUpdateStatus()
    }

    override fun refreshData() {
        appId = getAppIdByUrl()
    }

    // 获取已安装版本号
    val installedVersionNumber: String?
        get() = IoApi.getAppVersionNumber(this.appDatabase.packageId)
}

fun App.getParentApplications(): Applications? = if (this.appDatabase.id != 0L) null
else AppManager.getApplications(hubUuid = appDatabase.hubUuid)

fun App.isIgnoreUpdate(): Boolean {
    val parentApplicationsDatabase = this.getParentApplications()?.appDatabase
            ?: return false
    val appId = this.appId ?: return false
    return parentApplicationsDatabase.getIgnoreVersionNumber(appId) != null
}

fun App.setIgnoreUpdate() {
    ignoreVersionNumber = FOREVER_IGNORE
    runBlocking { AppDatabaseManager.updateAppDatabase(appDatabase) }
}

fun App.removeIgnoreUpdate() {
    val appId = this.appId ?: return
    this.getParentApplications()?.appDatabase?.removeIgnore(appId) ?: kotlin.run {
        ignoreVersionNumber = null
        runBlocking {
            AppDatabaseManager.updateAppDatabase(appDatabase)
        }
    }
}
