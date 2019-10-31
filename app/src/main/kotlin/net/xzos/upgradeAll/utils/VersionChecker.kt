package net.xzos.upgradeAll.utils

import android.annotation.SuppressLint
import com.jaredrummler.android.shell.Shell
import net.xzos.upgradeAll.application.MyApplication
import net.xzos.upgradeAll.json.gson.VersionCheckerGson
import net.xzos.upgradeAll.server.ServerContainer
import org.apache.maven.artifact.versioning.DefaultArtifactVersion
import java.util.regex.Pattern

class VersionChecker(private val versionCheckerGson: VersionCheckerGson?) {

    val version: String?
        get() {
            val versionCheckerApi: String? = versionCheckerGson?.api
            var version: String? = null
            if (versionCheckerApi != null) {
                val shellCommand: String? = versionCheckerGson?.text

                if (shellCommand != null)
                    @SuppressLint("DefaultLocale")
                    when (versionCheckerApi.toLowerCase()) {
                        "app" -> version = getAppVersion()
                        "magisk" -> version = getMagiskModuleVersion()
                        "shell" -> version = Shell.run(shellCommand).getStdout()
                        "shell_root" -> version = Shell.SU.run(shellCommand).getStdout()
                    }
            }
            return version
        }

    private fun getAppVersion(): String? {
        // 获取软件版本
        return try {
            val packageInfo = MyApplication.context.packageManager.getPackageInfo(versionCheckerGson?.text!!, 0)
            packageInfo.versionName
        } catch (e: Throwable) {
            null
        }
    }

    private fun getMagiskModuleVersion(): String? {
        var magiskModuleVersion: String? = null
        val modulePropFilePath = "/data/adb/modules/${versionCheckerGson?.text}/module.prop"
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

        private const val TAG = "VersionCheckerGson"
        private val LogObjectTag = arrayOf("Core", TAG)
        private val Log = ServerContainer.Log

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
            Log.i(LogObjectTag, TAG, String.format("compareVersionNumber: versionNumber0: %s, versionNumber1: %s", versionNumber0, versionNumber1))
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
