package net.xzos.upgradeall.utils.install

import net.xzos.upgradeall.core.data.json.nongson.ObjectTag
import net.xzos.upgradeall.core.log.Log
import net.xzos.upgradeall.utils.Shell
import net.xzos.upgradeall.utils.getErrorsString
import net.xzos.upgradeall.utils.getOutputString
import java.io.File


object ApkRootInstall {

    private const val TAG = "ApkRootInstall"
    private val logObjectTag = ObjectTag(ObjectTag.core, TAG)

    suspend fun install(file: File) {
        rowInstall(file)
    }

    suspend fun multipleInstall(apkFileList: List<File>) {
        // 参考: https://stackoverflow.com/questions/55212788/is-it-possible-to-merge-install-split-apk-files-aka-app-bundle-on-android-d
        if (apkFileList.isEmpty()) return
        var apkString = ""
        for (filePath in apkFileList)
            apkString += "${filePath.path} "
        val command = "adb install-multiple $apkString"
        runSuShellCommand(command)
    }

    suspend fun obbInstall(obbFileList: List<File>) {
        // 参考: https://stackoverflow.com/questions/55212788/is-it-possible-to-merge-install-split-apk-files-aka-app-bundle-on-android-d
        for (obbFile in obbFileList) {
            val delimiterIndexList = mutableListOf<Int>()
            val fileName = obbFile.name
            var index: Int = fileName.indexOf('.')
            while (index >= 0) {
                println(index)
                index = fileName.indexOf('.', index + 1)
            }
            val obbPackageName = fileName.subSequence(delimiterIndexList[1] + 1, delimiterIndexList.last())
            val command = "mv $obbFile /storage/emulated/0/Android/obb/$obbPackageName/."
            runSuShellCommand(command)
        }
    }

    private fun rowInstall(file: File) {
        val command = "cat ${file.path} | pm install -S ${file.length()}"
        runSuShellCommand(command)
    }

    private fun runSuShellCommand(command: String) {
        try {
            Shell.runSuShellCommand(command)?.also {
                if (it.exitCode != 0)
                    Log.e(logObjectTag, TAG, """
                    Error: out: ${it.getOutputString()}, err: ${it.getErrorsString()}
                """.trimIndent())
            }
        } catch (e: Throwable) {
            Log.e(logObjectTag, TAG, e.toString())
        }
    }
}
