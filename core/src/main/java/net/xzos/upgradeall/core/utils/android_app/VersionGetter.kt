package net.xzos.upgradeall.core.utils.android_app

import net.xzos.upgradeall.core.coreConfig
import net.xzos.upgradeall.core.data.ANDROID_APP_TYPE
import net.xzos.upgradeall.core.data.ANDROID_CUSTOM_SHELL
import net.xzos.upgradeall.core.data.ANDROID_CUSTOM_SHELL_ROOT
import net.xzos.upgradeall.core.data.ANDROID_MAGISK_MODULE_TYPE
import net.xzos.upgradeall.core.utils.shell.Shell
import net.xzos.upgradeall.core.utils.shell.getOutputString
import net.xzos.upgradeall.core.utils.shell.getProp


fun Map<String, String?>.getPackageId(): Pair<String, String>? {
    var api: String? = null
    val key = this[ANDROID_APP_TYPE]?.also { api = ANDROID_APP_TYPE }
            ?: this[ANDROID_MAGISK_MODULE_TYPE]?.also { api = ANDROID_MAGISK_MODULE_TYPE }
            ?: this[ANDROID_CUSTOM_SHELL]?.also { api = ANDROID_CUSTOM_SHELL }
            ?: this[ANDROID_CUSTOM_SHELL_ROOT]?.also { api = ANDROID_CUSTOM_SHELL_ROOT }
            ?: return null
    return Pair(api!!, key)
}

internal fun getAppVersion(appId: Map<String, String?>): String? {
    val (api, key) = appId.getPackageId() ?: return null
    return when (api.toLowerCase(coreConfig.locale)) {
        ANDROID_APP_TYPE -> getAndroidAppVersion(key)
        ANDROID_MAGISK_MODULE_TYPE -> getMagiskModuleVersion(key)
        ANDROID_CUSTOM_SHELL -> Shell.runShellCommand(key)?.getOutputString()
        ANDROID_CUSTOM_SHELL_ROOT -> Shell.runSuShellCommand(key)?.getOutputString()
        else -> null
    }
}


private fun getAndroidAppVersion(packageName: String): String? {
    // 获取软件版本
    return try {
        val packageInfo = coreConfig.androidContext.packageManager.getPackageInfo(packageName, 0)
        packageInfo.versionName
    } catch (e: Throwable) {
        null
    }
}

private fun getMagiskModuleVersion(moduleId: String): String? {
    val modulePropFilePath = "/data/adb/modules/$moduleId/module.prop"
    return getProp(modulePropFilePath)?.getProperty("version")
}