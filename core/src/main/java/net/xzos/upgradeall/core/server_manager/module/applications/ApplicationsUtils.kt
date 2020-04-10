package net.xzos.upgradeall.core.server_manager.module.applications

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.xzos.upgradeall.core.data.config.AppType
import net.xzos.upgradeall.core.data.database.AppDatabase
import net.xzos.upgradeall.core.data.json.gson.AppConfigGson
import net.xzos.upgradeall.core.data.json.gson.AppConfigGson.AppConfigBean.TargetCheckerBean.Companion.API_TYPE_APP_PACKAGE
import net.xzos.upgradeall.core.data_manager.utils.AutoTemplate
import net.xzos.upgradeall.core.route.AppIdItem
import net.xzos.upgradeall.core.server_manager.module.app.App
import net.xzos.upgradeall.core.system_api.api.IoApi

internal class ApplicationsUtils(applacationsDatabase: AppDatabase) {

    private val hubUuid = applacationsDatabase.hubUuid
    private val appUrlTemplate = applacationsDatabase.url
    private val excludePackageName = applacationsDatabase.extraData?.applicationsAutoExclude
            ?: mutableListOf()

    private val appType = AppType.androidApp
    // 暂时锁定应用市场模式为 android 应用

    private val appInfos: List<AppInfo> = IoApi.getAppInfoList(appType) ?: listOf()

    internal val apps: MutableList<App> = mutableListOf()
    internal val excludeApps: MutableList<App> = mutableListOf()
    private val dataMutex = Mutex()

    private suspend fun MutableList<App>.addApp(app: App): Boolean {
        return dataMutex.withLock {
            this.add(app)
        }
    }

    init {
        runBlocking {
            for (packageInfo in appInfos) {
                launch(Dispatchers.IO) {
                    val app = App(getAppDatabaseClass(packageInfo)).apply {
                        this.appId = listOf(AppIdItem.newBuilder()
                                .setKey(packageInfo.type)
                                .setValue(packageInfo.id)
                                .build()
                        )
                    }
                    if (!excludePackageName.contains(packageInfo.id))
                        apps.addApp(app)
                    else excludeApps.addApp(app)
                }
            }
        }
    }

    private fun getAppDatabaseClass(appInfo: AppInfo): AppDatabase {
        val name = appInfo.name
        val packageName = appInfo.id
        val packageType = appInfo.type
        val url = AutoTemplate.fillArgs(
                appUrlTemplate,
                listOf(AutoTemplate.Arg("%$packageType", packageName))
        )
        val type = AppDatabase.APP_TYPE_TAG
        return AppDatabase(0L, name, url, hubUuid, type).apply {
            targetChecker = AppConfigGson.AppConfigBean.TargetCheckerBean(
                    API_TYPE_APP_PACKAGE, packageName
            )
        }
    }
}

class AppInfo(
        val type: String,
        val name: String,
        val id: String
)
