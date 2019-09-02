package net.xzos.upgradeAll.utils

import android.annotation.SuppressLint
import android.content.pm.PackageManager

import com.jaredrummler.android.shell.Shell

import net.xzos.upgradeAll.application.MyApplication
import net.xzos.upgradeAll.server.ServerContainer

import org.apache.maven.artifact.versioning.DefaultArtifactVersion
import org.json.JSONException
import org.json.JSONObject

import java.util.regex.Pattern

class VersionChecker(private val inputVersionCheckerString: String? = null, private val inputVersionCheckerJsonObject: JSONObject? = null) {

    val version: String?
        get() {
            val versionCheckerJsonObject =
                    when {
                        inputVersionCheckerString != null -> JSONObject(inputVersionCheckerString)
                        inputVersionCheckerJsonObject != null -> inputVersionCheckerJsonObject
                        else -> null
                    } ?: return null
            var versionCheckerApi: String? = null
            var version: String? = null
            try {
                versionCheckerApi = versionCheckerJsonObject.getString("api")
            } catch (e: JSONException) {
                e.printStackTrace()
            }

            if (versionCheckerApi != null) {
                val shellCommand: String = try {
                    versionCheckerJsonObject.getString("text")
                } catch (e: JSONException) {
                    Log.e(LogObjectTag, TAG, "getVersion: JSONObject解析出错,  versionCheckerJsonObject: $versionCheckerJsonObject")
                    ""
                }

                @SuppressLint("DefaultLocale")
                when (versionCheckerApi.toLowerCase()) {
                    "app" -> version = getAppVersion(versionCheckerJsonObject)
                    "magisk" -> version = getMagiskModuleVersion(versionCheckerJsonObject)
                    "shell" -> version = Shell.run(shellCommand).getStdout()
                    "shell_root" -> version = Shell.SU.run(shellCommand).getStdout()
                }
            }
            return version
        }

    private fun getAppVersion(versionCheckerJsonObject: JSONObject): String? {
        // 获取软件版本
        var appVersion: String? = null
        var packageName: String? = null
        try {
            packageName = versionCheckerJsonObject.getString("text")
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        if (packageName != null) {
            val context = MyApplication.context
            appVersion = try {
                val packageInfo = context.packageManager.getPackageInfo(packageName, 0)
                packageInfo.versionName
            } catch (e: PackageManager.NameNotFoundException) {
                null
            }

        }
        return appVersion
    }

    private fun getMagiskModuleVersion(versionCheckerJsonObject: JSONObject): String? {
        var magiskModuleVersion: String? = null
        var magiskModuleName: String? = null
        try {
            magiskModuleName = versionCheckerJsonObject.getString("text")
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        val modulePropFilePath = "/data/adb/modules/$magiskModuleName/module.prop"
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

    companion object {

        private const val TAG = "VersionChecker"
        private val LogObjectTag = arrayOf("Core", TAG)
        private val Log = ServerContainer.AppServer.log

        private fun getVersionNumberString(versionString: String?): String? {
            var versionMatchString: String? = null
            val regexString = "(\\d+(\\.\\d+)*)(([-|_|.]|[0-9A-Za-z])*)"
            if (versionString != null) {
                val p = Pattern.compile(regexString)
                val m = p.matcher(versionString)
                versionMatchString = if (m.find()) {
                    m.group()
                } else
                    versionString
            }
            return versionMatchString
        }

        /**
         * 对比 versionNumber0 与 versionNumber1
         * 若，前者比后者大，则返回 true*/
        fun compareVersionNumber(versionNumber0: String?, versionNumber1: String?): Boolean {
            @Suppress("NAME_SHADOWING")
            var versionNumber0 = versionNumber0
            @Suppress("NAME_SHADOWING")
            var versionNumber1 = versionNumber1
            Log.i(LogObjectTag, TAG, String.format("compareVersionNumber: versionNumber0: %s , versionNumber1: %s", versionNumber0, versionNumber1))
            versionNumber0 = getVersionNumberString(versionNumber0)
            versionNumber1 = getVersionNumberString(versionNumber1)
            if (versionNumber0 != null && versionNumber1 != null) {
                val version0 = DefaultArtifactVersion(versionNumber0)
                val version1 = DefaultArtifactVersion(versionNumber1)
                return version0 >= version1
            }
            return false
        }
    }
}
