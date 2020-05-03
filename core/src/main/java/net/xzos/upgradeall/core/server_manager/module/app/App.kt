package net.xzos.upgradeall.core.server_manager.module.app

import net.xzos.upgradeall.core.data.config.AppType
import net.xzos.upgradeall.core.data.database.AppDatabase
import net.xzos.upgradeall.core.data.json.gson.AppDatabaseExtraData
import net.xzos.upgradeall.core.data.json.gson.getIgnoreApp
import net.xzos.upgradeall.core.data_manager.HubDatabaseManager
import net.xzos.upgradeall.core.data_manager.utils.AutoTemplate
import net.xzos.upgradeall.core.route.AppIdItem
import net.xzos.upgradeall.core.server_manager.AppManager
import net.xzos.upgradeall.core.server_manager.module.BaseApp
import net.xzos.upgradeall.core.system_api.api.IoApi

class App(override val appDatabase: AppDatabase) : BaseApp {

    val hubDatabase = HubDatabaseManager.getDatabase(appDatabase.hubUuid)
    var appId: List<AppIdItem>? = null
        get() {
            if (field != null) return field
            if (hubDatabase != null)
                for (appUrlTemplates in hubDatabase.hubConfig.appUrlTemplates) {
                    val args = AutoTemplate(appDatabase.url, appUrlTemplates).args.map {
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
            return null
        }

    private val packageName: String?
        get() {
            val appId = this.appId ?: return null
            for (appIdItem in appId) {
                if (appIdItem.key == AppType.androidApp) {
                    return appIdItem.value
                }
            }
            return null
        }

    var markProcessedVersionNumber: String?
        get() {
            return if (appDatabase.id != 0L)
                appDatabase.extraData?.markProcessedVersionNumber
            else {
                val applicationsDatabase = AppManager.getApplications(hubUuid = appDatabase.hubUuid)?.appDatabase
                        ?: return null
                applicationsDatabase.extraData!!.applicationsConfig!!.ignoreAppList!!.getIgnoreApp(packageName)
                        ?.versionNumber
            }
        }
        set(value) {
            val versionNumber = if (markProcessedVersionNumber != value)
                value
            else null
            if (appDatabase.id != 0L) {
                appDatabase.extraData?.markProcessedVersionNumber = versionNumber
                appDatabase.save(false)
            } else {
                val packageName = this.packageName ?: return
                val applicationsDatabase = AppManager.getApplications(hubUuid = appDatabase.hubUuid)?.appDatabase
                        ?: return
                val ignoreAppList = applicationsDatabase.extraData!!.applicationsConfig!!.ignoreAppList!!
                if (versionNumber != null) {
                    ignoreAppList.getIgnoreApp(packageName)?.also {
                        it.versionNumber = versionNumber
                    } ?: AppDatabaseExtraData.ApplicationsConfig.IgnoreApp(
                            packageName, false, versionNumber
                    ).let {
                        ignoreAppList.add(it)
                    }
                } else {
                    ignoreAppList.remove(ignoreAppList.getIgnoreApp(packageName))
                }
                applicationsDatabase.save(false)
            }
        }

    override suspend fun getUpdateStatus(): Int {
        return Updater(this).getUpdateStatus()
    }

    // 获取已安装版本号
    val installedVersionNumber: String?
        get() = IoApi.getAppVersionNumber(this.appDatabase.targetChecker)
}
