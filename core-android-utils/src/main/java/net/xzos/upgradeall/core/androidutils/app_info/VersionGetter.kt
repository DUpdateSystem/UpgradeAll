package net.xzos.upgradeall.core.androidutils.app_info

import android.content.Context
import android.os.Build
import net.xzos.upgradeall.core.androidutils.androidContext
import net.xzos.upgradeall.core.androidutils.getProp
import net.xzos.upgradeall.core.androidutils.locale
import net.xzos.upgradeall.core.shell.CoreShell
import net.xzos.upgradeall.core.shell.ShellResult
import net.xzos.upgradeall.core.shell.getOutputString
import net.xzos.upgradeall.core.utils.constant.*


fun Map<String, String?>.getPackageId(): Pair<String, String>? {
    var api: String? = null
    val key = this[ANDROID_APP_TYPE]?.also { api = ANDROID_APP_TYPE }
        ?: this[ANDROID_MAGISK_MODULE_TYPE]?.also { api = ANDROID_MAGISK_MODULE_TYPE }
        ?: this[ANDROID_CUSTOM_SHELL]?.also { api = ANDROID_CUSTOM_SHELL }
        ?: this[ANDROID_CUSTOM_SHELL_ROOT]?.also { api = ANDROID_CUSTOM_SHELL_ROOT }
        ?: return null
    return Pair(api!!, key)
}

fun getAppVersion(appId: Map<String, String?>, context: Context = androidContext): AppVersionInfo? {
    val (api, key) = appId.getPackageId() ?: return null
    return when (api.lowercase(locale)) {
        ANDROID_APP_TYPE -> getAndroidAppVersion(key, context)
        ANDROID_MAGISK_MODULE_TYPE -> getMagiskModuleVersion(key)
        ANDROID_CUSTOM_SHELL -> CoreShell.runShellCommand(key).toVersionInfo()
        ANDROID_CUSTOM_SHELL_ROOT -> CoreShell.runSuShellCommand(key).toVersionInfo()
        else -> null
    }
}

private fun ShellResult.toVersionInfo() = AppVersionInfo(getOutputString())

private fun getAndroidAppVersion(packageName: String, context: Context): AppVersionInfo? {
    // 获取软件版本
    return try {
        val packageInfo = context.packageManager.getPackageInfo(packageName, 0)
        AppVersionInfo(
            packageInfo.versionName ?: "", mapOf(
                VERSION_CODE to
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                            packageInfo.longVersionCode
                        else packageInfo.versionCode
            )
        )
    } catch (e: Throwable) {
        null
    }
}

private fun getMagiskModuleVersion(moduleId: String): AppVersionInfo? {
    val modulePropFilePath = "/data/adb/modules/$moduleId/module.prop"
    return AppVersionInfo(getProp(modulePropFilePath)?.getProperty("version") ?: return null)
}