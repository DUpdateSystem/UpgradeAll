package net.xzos.upgradeall.core.server_manager.module.app

import net.xzos.upgradeall.core.data.config.AppType
import net.xzos.upgradeall.core.data.database.AppDatabase
import net.xzos.upgradeall.core.data.json.gson.addIgnoreAppList
import net.xzos.upgradeall.core.data.json.gson.getIgnoreApp
import net.xzos.upgradeall.core.data.json.gson.removeIgnoreAppList
import net.xzos.upgradeall.core.data_manager.HubDatabaseManager
import net.xzos.upgradeall.core.data_manager.utils.AutoTemplate
import net.xzos.upgradeall.core.data_manager.utils.VersioningUtils.IGNORE_VERSION
import net.xzos.upgradeall.core.route.AppIdItem
import net.xzos.upgradeall.core.server_manager.AppManager
import net.xzos.upgradeall.core.server_manager.module.BaseApp
import net.xzos.upgradeall.core.server_manager.module.applications.Applications
import net.xzos.upgradeall.core.system_api.api.IoApi

class App(override val appDatabase: AppDatabase) : BaseApp {

    val hubDatabase = HubDatabaseManager.getDatabase(appDatabase.hubUuid)
    var appId: List<AppIdItem>? = null
        get() {
            if (field != null) return field
            if (hubDatabase != null) {
                if (hubDatabase.hubConfig.apiKeywords.isEmpty())
                    return listOf()
                for (appUrlTemplate in hubDatabase.hubConfig.appUrlTemplates) {
                    val args = AutoTemplate(appDatabase.url, appUrlTemplate).args.map {
                        AutoTemplate.Arg(it.key.substringAfterLast("%"), it.value)
                    }.filter { it.value.isNotBlank() }
                    val keys = args.map { it.key }
                    if (keys.containsAll(hubDatabase.hubConfig.apiKeywords))
                        return args.map {
                            AppIdItem.newBuilder().setKey(it.key).setValue(it.value).build()
                        }.also {
                            field = it
                        }
                }
            }
            return null
        }

    internal val packageName: String?
        get() {
            val appId = this.appId ?: return null
            for (appIdItem in appId) {
                if (appIdItem.key == AppType.androidApp) {
                    return appIdItem.value
                }
            }
            return null
        }

    val markProcessedVersionNumber: String?
        get() {
            val parentApplicationsDatabase = this.getParentApplications()?.appDatabase
                    ?: return appDatabase.extraData!!.markProcessedVersionNumber
            val ignoreApp = parentApplicationsDatabase.extraData!!.getIgnoreApp(packageName)
                    ?: return null
            return if (ignoreApp.forever)
                IGNORE_VERSION
            else ignoreApp.versionNumber
        }

    override suspend fun getUpdateStatus(): Int {
        return Updater(this).getUpdateStatus()
    }

    // 获取已安装版本号
    val installedVersionNumber: String?
        get() = IoApi.getAppVersionNumber(this.appDatabase.targetChecker)
}

fun App.getParentApplications(): Applications? = if (this.appDatabase.id != 0L) null
else AppManager.getApplications(hubUuid = appDatabase.hubUuid)

fun App.isIgnoreUpdate(): Boolean {
    val parentApplicationsDatabase = this.getParentApplications()?.appDatabase
            ?: return false
    return parentApplicationsDatabase.extraData!!.getIgnoreApp(this@isIgnoreUpdate.packageName) != null
}

fun App.setIgnoreUpdate(versionNumber: String) {
    this.getParentApplications()?.appDatabase?.apply {
        extraData!!.addIgnoreAppList(packageName ?: return, false, versionNumber)
        save(false)
    } ?: kotlin.run {
        appDatabase.extraData!!.markProcessedVersionNumber = versionNumber
        appDatabase.save(false)
    }
}

fun App.setIgnoreUpdate() {
    this.getParentApplications()?.appDatabase?.apply {
        extraData!!.addIgnoreAppList(packageName ?: return, true, null)
        save(false)
    }
}

fun App.removeIgnoreUpdate() {
    this.getParentApplications()?.appDatabase?.apply {
        extraData!!.removeIgnoreAppList(packageName ?: return)
        save(false)
    } ?: kotlin.run {
        appDatabase.extraData!!.markProcessedVersionNumber = null
        appDatabase.save(false)
    }
}
