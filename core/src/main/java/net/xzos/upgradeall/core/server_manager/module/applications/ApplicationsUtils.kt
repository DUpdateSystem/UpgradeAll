package net.xzos.upgradeall.core.server_manager.module.applications

import net.xzos.upgradeall.core.data.config.AppType
import net.xzos.upgradeall.core.data.database.AppDatabase
import net.xzos.upgradeall.core.data.json.gson.AppConfigGson
import net.xzos.upgradeall.core.data.json.gson.AppConfigGson.AppConfigBean.TargetCheckerBean.Companion.API_TYPE_APP_PACKAGE
import net.xzos.upgradeall.core.data_manager.utils.AutoTemplate
import net.xzos.upgradeall.core.route.AppIdItem
import net.xzos.upgradeall.core.server_manager.module.app.App
import net.xzos.upgradeall.core.system_api.api.IoApi

internal class ApplicationsUtils(applicationsDatabase: AppDatabase) {

    private val hubUuid = applicationsDatabase.hubUuid
    private val appUrlTemplate = applicationsDatabase.url
    private val excludePackageName = applicationsDatabase.extraData?.applicationsConfig?.invalidPackageName
            ?: mutableListOf()

    private val appType = AppType.androidApp
    // 暂时锁定应用市场模式为 android 应用

    internal val apps: List<App>
        get() = allApp.filter {
            !excludePackageName.contains(it.appId?.get(0)?.value)
        }
    internal val excludeApps: List<App>
        get() = allApp.filter {
            excludePackageName.contains(it.appId?.get(0)?.value)
        }

    private val allApp: List<App> by lazy {
        IoApi.getAppInfoList(appType)?.map {
            App(it.toAppDatabaseClass()).apply {
                this.appId = listOf(AppIdItem.newBuilder()
                        .setKey(it.type)
                        .setValue(it.id)
                        .build()
                )
            }
        } ?: listOf()
    }

    private fun AppInfo.toAppDatabaseClass(): AppDatabase {
        val packageName = id
        val url = AutoTemplate.fillArgs(
                appUrlTemplate,
                listOf(AutoTemplate.Arg("%$type", packageName))
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
