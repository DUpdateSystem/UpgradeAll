package net.xzos.upgradeall.core.androidutils.app_info

import android.content.Context
import net.xzos.upgradeall.core.androidutils.androidContext
import net.xzos.upgradeall.core.androidutils.getProp
import net.xzos.upgradeall.core.androidutils.locale
import net.xzos.upgradeall.core.shell.Shell
import net.xzos.upgradeall.core.shell.getOutputString
import net.xzos.upgradeall.core.utils.constant.ANDROID_APP_TYPE
import net.xzos.upgradeall.core.utils.constant.ANDROID_CUSTOM_SHELL
import net.xzos.upgradeall.core.utils.constant.ANDROID_CUSTOM_SHELL_ROOT
import net.xzos.upgradeall.core.utils.constant.ANDROID_MAGISK_MODULE_TYPE


fun Map<String, String?>.getPackageId(): Pair<String, String>? {
    var api: String? = null
    val key = this[ANDROID_APP_TYPE]?.also { api = ANDROID_APP_TYPE }
            ?: this[ANDROID_MAGISK_MODULE_TYPE]?.also { api = ANDROID_MAGISK_MODULE_TYPE }
            ?: this[ANDROID_CUSTOM_SHELL]?.also { api = ANDROID_CUSTOM_SHELL }
            ?: this[ANDROID_CUSTOM_SHELL_ROOT]?.also { api = ANDROID_CUSTOM_SHELL_ROOT }
            ?: return null
    return Pair(api!!, key)
}

fun getAppVersion(appId: Map<String, String?>, context: Context = androidContext): String? {
    val (api, key) = appId.getPackageId() ?: return null
    return when (api.lowercase(locale)) {
        ANDROID_APP_TYPE -> getAndroidAppVersion(key, context)
        ANDROID_MAGISK_MODULE_TYPE -> getMagiskModuleVersion(key)
        ANDROID_CUSTOM_SHELL -> Shell.runShellCommand(key)?.getOutputString()
        ANDROID_CUSTOM_SHELL_ROOT -> Shell.runSuShellCommand(key)?.getOutputString()
        else -> null
    }
}


private fun getAndroidAppVersion(packageName: String, context: Context): String? {
    // 获取软件版本
    return try {
        val packageInfo = context.packageManager.getPackageInfo(packageName, 0)
        packageInfo.versionName
    } catch (e: Throwable) {
        null
    }
}

private fun getMagiskModuleVersion(moduleId: String): String? {
    val modulePropFilePath = "/data/adb/modules/$moduleId/module.prop"
    return getProp(modulePropFilePath)?.getProperty("version")
}