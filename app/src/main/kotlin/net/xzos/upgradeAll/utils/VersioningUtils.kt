package net.xzos.upgradeAll.utils

import android.annotation.SuppressLint
import com.google.gson.Gson
import com.jaredrummler.android.shell.Shell
import net.xzos.upgradeAll.R
import net.xzos.upgradeAll.application.MyApplication
import net.xzos.upgradeAll.data.json.gson.AppConfig
import net.xzos.upgradeAll.data.json.gson.VersionCheckerGson
import net.xzos.upgradeAll.server.ServerContainer
import org.apache.maven.artifact.versioning.DefaultArtifactVersion

object VersioningUtils {

    private const val TAG = "VersionCheckerGson"
    private val logObjectTag = arrayOf("Core", TAG)
    private val Log = ServerContainer.Log

    internal fun matchVersioningString(versionString: CharSequence?): String? {
        return if (versionString != null) {
            val regexString = MyApplication.context.getString(R.string.versioning_regex_match)
            val regex = regexString.toRegex()
            regex.find(versionString)?.value
        } else null
    }

    /**
     * 对比 versionNumber0 与 versionNumber1
     * 若，前者比后者大，则返回 true*/
    internal fun compareVersionNumber(versioning0: String?, versioning1: String?): Boolean {
        val matchVersioning0 = matchVersioningString(versioning0)
        val matchVersioning1 = matchVersioningString(versioning1)
        Log.i(logObjectTag, TAG,
                """original versioning:
                0: $versioning0, 1: $versioning1
                0: $matchVersioning0, 1: $matchVersioning1"""
        )
        if (matchVersioning0 != null && matchVersioning1 != null) {
            val version0 = DefaultArtifactVersion(matchVersioning0)
            val version1 = DefaultArtifactVersion(matchVersioning1)
            return version0 >= version1
        }
        return false
    }

    /** 修补老标准格式
     * TODO: 修改版本: 0.1.0-alpha.beta
     */
    @SuppressLint("DefaultLocale")
    fun fixJson(jsonString: String): AppConfig.AppConfigBean.TargetCheckerBean {
        return AppConfig.AppConfigBean.TargetCheckerBean().apply {
            val versionCheckerGson = Gson().fromJson(jsonString, VersionCheckerGson::class.java)
            val extraString = versionCheckerGson.text
            val versionCheckerApi = versionCheckerGson.api
            if (extraString != null && versionCheckerApi != null) {
                this.extraString = versionCheckerGson.text
                this.api = when (versionCheckerApi.toLowerCase()) {
                    "app" -> "App_Package"
                    "magisk" -> "Magisk_Module"
                    else -> versionCheckerApi
                }
            }
        }
    }

    class VersionChecker(private val targetCheckerGson: AppConfig.AppConfigBean.TargetCheckerBean?) {

        val version: String?
            get() {
                val versionCheckerApi: String? = targetCheckerGson?.api
                var version: String? = null
                if (versionCheckerApi != null) {
                    val shellCommand: String? = targetCheckerGson?.extraString

                    if (shellCommand != null)
                        @SuppressLint("DefaultLocale")
                        when (versionCheckerApi.toLowerCase()) {
                            "app_package" -> version = getAppVersion()
                            "magisk_module" -> version = getMagiskModuleVersion()
                            "shell" -> version = Shell.run(shellCommand).getStdout()
                            "shell_root" -> version = Shell.SU.run(shellCommand).getStdout()
                        }
                }
                return version
            }

        private fun getAppVersion(): String? {
            // 获取软件版本
            return try {
                val packageInfo = MyApplication.context.packageManager.getPackageInfo(targetCheckerGson?.extraString!!, 0)
                packageInfo.versionName
            } catch (e: Throwable) {
                null
            }
        }

        private fun getMagiskModuleVersion(): String? {
            var magiskModuleVersion: String? = null
            val modulePropFilePath = "/data/adb/modules/${targetCheckerGson?.extraString}/module.prop"
            val command = "cat $modulePropFilePath"
            val result = Shell.SU.run(command)
            val resultList = result.getStdout().split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val keyWords = "version="
            for (resultLine in resultList) {
                if (resultLine.indexOf(keyWords) == 0) {
                    magiskModuleVersion = resultLine.substring(keyWords.length)
                }
            }
            return magiskModuleVersion
        }
    }
}