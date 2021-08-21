package net.xzos.upgradeall.core.utils.android_app

import android.annotation.SuppressLint
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import net.xzos.upgradeall.core.androidutils.androidContext
import net.xzos.upgradeall.core.coreConfig
import net.xzos.upgradeall.core.data.ANDROID_APP_TYPE
import net.xzos.upgradeall.core.data.ANDROID_MAGISK_MODULE_TYPE
import net.xzos.upgradeall.core.database.table.AppEntity
import net.xzos.upgradeall.core.shell.getFileNameList
import net.xzos.upgradeall.core.utils.getProp


private const val MODULE_FOLDER_PATH = "/data/adb/modules/"

@SuppressLint("QueryPermissionsNeeded")
fun getAndroidAppInfoList(): List<AppInfo> {
    val pm = androidContext.packageManager
    return pm.getInstalledApplications(PackageManager.GET_META_DATA).mapNotNull {
        if (coreConfig.applications_ignore_system_app)
            if (it.flags and ApplicationInfo.FLAG_SYSTEM != 0)
                return@mapNotNull null
        val name = pm.getApplicationLabel(it)
        AppInfo(name.toString(), mapOf(ANDROID_APP_TYPE to it.packageName))
    }
}

fun getAndroidModuleInfoList(): List<AppInfo> {
    return getFileNameList(MODULE_FOLDER_PATH).mapNotNull { moduleId ->
        val modulePropFilePath = "/data/adb/modules/$moduleId/module.prop"
        val prop = getProp(modulePropFilePath) ?: return@mapNotNull null
        AppInfo(
            prop.getProperty("name"),
            mapOf(ANDROID_MAGISK_MODULE_TYPE to prop.getProperty("id"))
        )
    }
}

class AppInfo(
    val name: String,
    val idMap: Map<String, String>,
)

fun AppInfo.toAppEntity(): AppEntity {
    return AppEntity(0, name, idMap)
}