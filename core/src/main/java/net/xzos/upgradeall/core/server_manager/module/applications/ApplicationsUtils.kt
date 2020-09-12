package net.xzos.upgradeall.core.server_manager.module.applications

import net.xzos.upgradeall.core.data.config.AppType
import net.xzos.upgradeall.core.data.database.AppDatabase
import net.xzos.upgradeall.core.data.database.ApplicationsDatabase
import net.xzos.upgradeall.core.data.json.gson.PackageIdGson
import net.xzos.upgradeall.core.data.json.gson.PackageIdGson.Companion.API_TYPE_APP_PACKAGE
import net.xzos.upgradeall.core.data_manager.HubDatabaseManager
import net.xzos.upgradeall.core.data_manager.utils.AutoTemplate
import net.xzos.upgradeall.core.server_manager.module.app.App
import net.xzos.upgradeall.core.system_api.api.IoApi

internal class ApplicationsUtils(applicationsDatabase: ApplicationsDatabase) {

    private val hubUuid = applicationsDatabase.hubUuid
    private val appUrlTemplate = HubDatabaseManager.getDatabase(hubUuid)
            ?.hubConfig?.appUrlTemplates?.get(0) ?: ""
    private val auth = applicationsDatabase.auth
    private val excludePackageName = applicationsDatabase.invalidPackageList

    private val appType = AppType.androidApp
    // 暂时锁定应用市场模式为 android 应用

    internal val apps: List<App>
        get() = allApp.filter {
            !excludePackageName.contains(it.appId)
        }
    internal val excludeApps: List<App>
        get() = allApp.filter {
            excludePackageName.contains(it.appId)
        }

    private val allApp: List<App> by lazy {
        IoApi.getAppInfoList(appType)?.map {
            App(it.toAppDatabaseClass(), mapOf(it.type to it.id))
        } ?: listOf()
    }

    private fun AppInfo.toAppDatabaseClass(): AppDatabase {
        val packageName = id
        val url = AutoTemplate.fillArgs(appUrlTemplate, mapOf("%$type" to packageName))
        return AppDatabase(0, name, hubUuid, url, PackageIdGson(API_TYPE_APP_PACKAGE, packageName), auth = auth)
    }
}

class AppInfo(
        val type: String,
        val name: String,
        val id: String
)
