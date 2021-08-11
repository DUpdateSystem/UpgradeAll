package net.xzos.upgradeall.core.server_manager.module.app

import kotlinx.coroutines.runBlocking
import net.xzos.upgradeall.core.data.database.AppDatabase
import net.xzos.upgradeall.core.data.json.gson.ReleaseGson
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
    var appId: Map<String, String?>? = appId ?: getAppIdByUrl()
        private set

    private val updater by lazy { Updater(this) }
    private fun getAppIdByUrl(): Map<String, String?>? {
        if (hubDatabase != null) {
            val apiKeywords = hubDatabase.hubConfig.apiKeywords
            if (apiKeywords.isEmpty())
                return mapOf()
            for (appUrlTemplate in hubDatabase.hubConfig.appUrlTemplates) {
                val m = AutoTemplate(appDatabase.url, appUrlTemplate).args.mapKeys {
                    it.key.substringAfterLast("%")
                }.filter { it.value.isNotBlank() } + appDatabase.extraId
                if (m.keys.toList() == apiKeywords)
                    return m
            }
        }
        return null
    }

    val ignoreVersionNumber: String?
        get() {
            val appId = this.appId ?: return null
            return getParentApplications()?.appDatabase?.getIgnoreVersionNumber(appId)
                    ?: appDatabase.ignoreVersionNumber
        }

    override fun refreshData() {
        appId = getAppIdByUrl()
    }

    // 获取已安装版本号
    val installedVersionNumber: String?
        get() = IoApi.getAppVersionNumber(this.appDatabase.packageId)

    override suspend fun getUpdateStatus(): Int {
        return updater.getUpdateStatus()
    }

    suspend fun getReleaseList(): List<ReleaseGson>? {
        return updater.getReleaseList()
    }

    suspend fun getLatestVersioning(): String? {
        return updater.getLatestVersioning()
    }

    suspend fun downloadReleaseFile(fileIndex: Pair<Int, Int>, externalDownloader: Boolean = false) {
        return updater.downloadReleaseFile(fileIndex, externalDownloader)
    }
}

fun App.getParentApplications(): Applications? = if (this.appDatabase.id != 0L) null
else AppManager.getApplications(hubUuid = appDatabase.hubUuid)

fun App.isIgnoreUpdate(): Boolean {
    val parentApplicationsDatabase = this.getParentApplications()?.appDatabase
            ?: return false
    val appId = this.appId ?: return false
    return parentApplicationsDatabase.getIgnoreVersionNumber(appId) == FOREVER_IGNORE
}

fun App.setIgnoreUpdate(versionNumber: String? = null) {
    val ignoreVersionNumber = versionNumber ?: FOREVER_IGNORE
    val appId = this.appId ?: return
    runBlocking {
        getParentApplications()?.appDatabase?.let {
            it.addIgnore(appId, ignoreVersionNumber)
            AppDatabaseManager.updateApplicationsDatabase(it)
        } ?: kotlin.run {
            appDatabase.ignoreVersionNumber = ignoreVersionNumber
            AppDatabaseManager.updateAppDatabase(appDatabase)
        }
    }
}

fun App.removeIgnoreUpdate() {
    val appId = this.appId ?: return
    runBlocking {
        getParentApplications()?.appDatabase?.let {
            it.removeIgnore(appId)
            AppDatabaseManager.updateAppDatabase(appDatabase)
        } ?: kotlin.run {
            appDatabase.ignoreVersionNumber = null
            AppDatabaseManager.updateAppDatabase(appDatabase)
        }
    }
}
