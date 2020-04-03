package net.xzos.upgradeall.utils

import net.xzos.upgradeall.core.data.config.AppConfig
import net.xzos.upgradeall.core.data.json.gson.AppConfigGson
import net.xzos.upgradeall.core.data.json.gson.AppConfigGson.AppConfigBean.TargetCheckerBean.Companion.API_TYPE_APP_PACKAGE
import net.xzos.upgradeall.core.data.json.gson.AppConfigGson.AppConfigBean.TargetCheckerBean.Companion.API_TYPE_MAGISK_MODULE
import net.xzos.upgradeall.core.data.json.gson.AppConfigGson.AppConfigBean.TargetCheckerBean.Companion.API_TYPE_SHELL
import net.xzos.upgradeall.core.data.json.gson.AppConfigGson.AppConfigBean.TargetCheckerBean.Companion.API_TYPE_SHELL_ROOT
import net.xzos.upgradeall.application.MyApplication


object VersioningUtils {

    fun getAppVersionNumber(
            targetChecker: AppConfigGson.AppConfigBean.TargetCheckerBean?
    ): String? = VersionChecker(targetChecker).version

    class VersionChecker(private val targetChecker: AppConfigGson.AppConfigBean.TargetCheckerBean?) {

        val version: String?
            get() {
                val versionCheckerApi: String? = targetChecker?.api
                var version: String? = null
                if (versionCheckerApi != null) {
                    val shellCommand: String? = targetChecker?.extraString

                    if (shellCommand != null)
                        version = when (versionCheckerApi.toLowerCase(AppConfig.locale)) {
                            API_TYPE_APP_PACKAGE -> getAppVersion()
                            API_TYPE_MAGISK_MODULE -> getMagiskModuleVersion()
                            API_TYPE_SHELL -> MiscellaneousUtils.runShellCommand(shellCommand)?.getStdout()
                            API_TYPE_SHELL_ROOT -> MiscellaneousUtils.runShellCommand(shellCommand, su = true)?.getStdout()
                            else -> null
                        }
                }
                return version
            }

        private fun getAppVersion(): String? {
            // 获取软件版本
            return try {
                val packageInfo = MyApplication.context.packageManager.getPackageInfo(targetChecker?.extraString!!, 0)
                packageInfo.versionName
            } catch (e: Throwable) {
                null
            }
        }

        private fun getMagiskModuleVersion(): String? {
            var magiskModuleVersion: String? = null
            val modulePropFilePath = "/data/adb/modules/${targetChecker?.extraString}/module.prop"
            val command = "cat $modulePropFilePath"
            MiscellaneousUtils.runShellCommand(command, su = true)?.let { result ->
                val resultList = result.getStdout().split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val keyWords = "version="
                for (resultLine in resultList) {
                    if (resultLine.indexOf(keyWords) == 0) {
                        magiskModuleVersion = resultLine.substring(keyWords.length)
                    }
                }
            }
            return magiskModuleVersion
        }
    }
}
