package net.xzos.upgradeall.core.androidutils.app_info

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import net.xzos.upgradeall.core.androidutils.getProp
import net.xzos.upgradeall.core.shell.getFileNameList


private const val MODULE_FOLDER_PATH = "/data/adb/modules/"

@SuppressLint("QueryPermissionsNeeded")
fun getAndroidAppInfoList(context: Context, ignoreSystemApp: Boolean): List<AppInfo> {
    val pm = context.packageManager
    return pm.getInstalledApplications(PackageManager.GET_META_DATA).mapNotNull {
        if (ignoreSystemApp)
            if (it.flags and ApplicationInfo.FLAG_SYSTEM != 0)
                return@mapNotNull null
        val name = pm.getApplicationLabel(it)
        AppInfo(name.toString(), mapOf(ANDROID_APP_TYPE to it.packageName))
    }
}

fun getAndroidModuleInfoList(): List<AppInfo> {
    return getFileNameList(MODULE_FOLDER_PATH).mapNotNull { moduleId ->
        val modulePropFilePath = "/data/adb/modules/$moduleId/module.prop"
        try {
            val prop = getProp(modulePropFilePath) ?: return@mapNotNull null
            AppInfo(
                prop.getProperty("name"),
                mapOf(ANDROID_MAGISK_MODULE_TYPE to prop.getProperty("id"))
            )
        } catch (e: Throwable) {
            null
        }
    }
}

class AppInfo(
    val name: String,
    val idMap: Map<String, String>,
)