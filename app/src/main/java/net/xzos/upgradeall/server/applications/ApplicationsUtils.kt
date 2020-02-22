package net.xzos.upgradeall.server.applications

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import net.xzos.upgradeall.application.MyApplication.Companion.context
import net.xzos.upgradeall.data.json.gson.AppConfigGson
import net.xzos.upgradeall.data.json.gson.AppConfigGson.AppConfigBean.TargetCheckerBean.Companion.API_TYPE_APP_PACKAGE
import net.xzos.upgradeall.data.json.gson.HubConfig
import net.xzos.upgradeall.data_manager.database.AppDatabase
import net.xzos.upgradeall.data_manager.utils.AutoTemplate
import net.xzos.upgradeall.server_manager.runtime.manager.module.app.App


internal class ApplicationsUtils(applacationDatabase: AppDatabase) {

    private val hubUuid = applacationDatabase.api_uuid
    private val appUrlTemplate = applacationDatabase.url

    private val packages: List<ApplicationInfo>
        get() = context.packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

    val apps: HashSet<App>
        get() {
            val list = hashSetOf<App>()
            for (packageInfo in packages) {
                list.add(App(getAppDatabaseClass(packageInfo)))
            }
            return list
        }

    private fun getAppDatabaseClass(appInfo: ApplicationInfo): AppDatabase {
        val name = appInfo.name
        val packageName = appInfo.packageName
        val url = AutoTemplate(null, appUrlTemplate).getString(
                listOf(AutoTemplate.Arg(HubConfig.APP_URL_TEMPLATE_APP_PACKAGE_API, packageName))
        )
        val type = HubConfig.APP_URL_TEMPLATE_APP_PACKAGE_API
        return AppDatabase(name, url, hubUuid, type).apply {
            targetChecker = AppConfigGson.AppConfigBean.TargetCheckerBean(
                    API_TYPE_APP_PACKAGE, packageName
            )
        }
    }
}
