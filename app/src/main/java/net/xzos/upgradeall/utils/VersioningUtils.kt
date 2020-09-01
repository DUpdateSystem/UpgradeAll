package net.xzos.upgradeall.utils

import net.xzos.upgradeall.application.MyApplication
import net.xzos.upgradeall.core.data.config.AppValue
import net.xzos.upgradeall.core.data.json.gson.PackageIdGson
import net.xzos.upgradeall.core.data.json.gson.PackageIdGson.Companion.API_TYPE_APP_PACKAGE
import net.xzos.upgradeall.core.data.json.gson.PackageIdGson.Companion.API_TYPE_MAGISK_MODULE
import net.xzos.upgradeall.core.data.json.gson.PackageIdGson.Companion.API_TYPE_SHELL
import net.xzos.upgradeall.core.data.json.gson.PackageIdGson.Companion.API_TYPE_SHELL_ROOT


object VersioningUtils {

    fun getAppVersionNumber(
            targetChecker: PackageIdGson?
    ): String? = VersionChecker(targetChecker).version

    class VersionChecker(private val packageId: PackageIdGson?) {

        val version: String?
            get() {
                val versionCheckerApi: String? = packageId?.api
                var version: String? = null
                if (versionCheckerApi != null) {
                    val shellCommand: String? = packageId?.extraString
                    if (shellCommand != null)
                        version = when (versionCheckerApi.toLowerCase(AppValue.locale)) {
                            API_TYPE_APP_PACKAGE -> getAppVersion()
                            API_TYPE_MAGISK_MODULE -> getMagiskModuleVersion()
                            API_TYPE_SHELL -> Shell.runShellCommand(shellCommand)?.getOutputString()
                            API_TYPE_SHELL_ROOT -> Shell.runSuShellCommand(shellCommand)?.getOutputString()
                            else -> null
                        }
                }
                return version
            }

        private fun getAppVersion(): String? {
            // 获取软件版本
            return try {
                val packageInfo = MyApplication.context.packageManager.getPackageInfo(packageId?.extraString!!, 0)
                packageInfo.versionName
            } catch (e: Throwable) {
                null
            }
        }

        private fun getMagiskModuleVersion(): String? {
            val modulePropFilePath = "/data/adb/modules/${packageId?.extraString}/module.prop"
            val command = "cat $modulePropFilePath"
            val fileString = Shell.runSuShellCommand(command)?.getOutputString() ?: return null
            val prop = MiscellaneousUtils.parsePropertiesString(fileString)
            return prop.getProperty("version", null)
        }
    }
}
