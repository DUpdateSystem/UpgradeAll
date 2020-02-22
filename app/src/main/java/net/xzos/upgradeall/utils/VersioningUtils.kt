package net.xzos.upgradeall.utils

import android.annotation.SuppressLint
import net.xzos.upgradeall.application.MyApplication
import net.xzos.upgradeall.data.json.gson.AppConfigGson
import net.xzos.upgradeall.system_api.annotations.IoApi


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
                        @SuppressLint("DefaultLocale")
                        when (versionCheckerApi.toLowerCase()) {
                            "app_package" -> version = getAppVersion()
                            "magisk_module" -> version = getMagiskModuleVersion()
                            "shell" -> version = MiscellaneousUtils.runShellCommand(shellCommand)?.getStdout()
                            "shell_root" -> version = MiscellaneousUtils.runShellCommand(shellCommand, su = true)?.getStdout()
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
