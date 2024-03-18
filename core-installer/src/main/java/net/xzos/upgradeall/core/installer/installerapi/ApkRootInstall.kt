package net.xzos.upgradeall.core.installer.installerapi

import android.content.Context
import net.xzos.upgradeall.core.androidutils.getCurrentUserId
import net.xzos.upgradeall.core.shell.CoreShell
import net.xzos.upgradeall.core.shell.ShellResult
import net.xzos.upgradeall.core.shell.getErrorsString
import net.xzos.upgradeall.core.shell.getOutputString
import net.xzos.upgradeall.core.utils.log.Log
import net.xzos.upgradeall.core.utils.log.ObjectTag
import net.xzos.upgradeall.core.utils.log.msg
import java.io.File


object ApkRootInstall {

    private const val TAG = "ApkRootInstall"
    private val logObjectTag = ObjectTag(ObjectTag.core, TAG)

    suspend fun install(file: File, userId: Long) {
        rowInstall(file, userId)
    }

    suspend fun multipleInstall(apkFileList: List<File>, userId: Long) {
        // 参考: https://stackoverflow.com/questions/55212788/is-it-possible-to-merge-install-split-apk-files-aka-app-bundle-on-android-d/55475988#55475988
        var totalSize: Long = 0
        for (file in apkFileList) {
            Log.d(logObjectTag, TAG, "multipleInstall: file: ${file.name}")
            totalSize += file.length()
        }
        val result = runSuShellCommand("pm install-create --user $userId -S $totalSize") ?: return
        val resultString = result.getOutputString()
        val sessionIdRegex = "[1-9]\\d*".toRegex()
        val sessionIdMatch = sessionIdRegex.find(resultString) ?: return
        val sessionId = sessionIdMatch.value.toLong()
        Log.d(logObjectTag, TAG, "multipleInstall: create session: $sessionId")
        for (file in apkFileList) {
            runSuShellCommand("pm install-write --user $userId -S ${file.length()} $sessionId ${file.name} ${file.absolutePath}")
            Log.d(
                logObjectTag,
                TAG,
                "multipleInstall: write session: $sessionId file: ${file.name}"
            )
        }
        runSuShellCommand("pm install-commit $sessionId")
        Log.d(logObjectTag, TAG, "multipleInstall: write commit: $sessionId")
    }

    suspend fun obbInstall(obbFileList: List<File>, userId: Long) {
        // 参考: https://stackoverflow.com/questions/55212788/is-it-possible-to-merge-install-split-apk-files-aka-app-bundle-on-android-d
        for (obbFile in obbFileList) {
            val delimiterIndexList = mutableListOf<Int>()
            val fileName = obbFile.name
            Log.d(logObjectTag, TAG, "multipleInstall: obb name: $fileName")
            var index: Int = fileName.indexOf('.')
            delimiterIndexList.add(index)
            while (true) {
                index = fileName.indexOf('.', index + 1)
                if (index >= 0)
                    delimiterIndexList.add(index)
                else
                    break
            }
            val obbPackageName =
                fileName.subSequence(delimiterIndexList[1] + 1, delimiterIndexList.last())
            val command = "mv $obbFile /storage/emulated/$userId/Android/obb/$obbPackageName/."
            Log.d(logObjectTag, TAG, "multipleInstall: obb command: $command")
            runSuShellCommand(command)
        }
    }

    private fun rowInstall(file: File, userId: Long) {
        val command = "cat ${file.path} | pm install --user $userId -S ${file.length()}"
        runSuShellCommand(command)
    }

    private fun runSuShellCommand(command: String): ShellResult? {
        return try {
            CoreShell.runSuShellCommand(command).also {
                if (it.exitCode != 0)
                    Log.e(
                        logObjectTag, TAG,
                        "Error: out: ${it.getOutputString()}, err: ${it.getErrorsString()}"
                    )
            }
        } catch (e: Throwable) {
            Log.e(logObjectTag, TAG, e.msg())
            throw e
        }
    }
}