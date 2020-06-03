package net.xzos.upgradeall.utils.install

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
        withContext(Dispatchers.Default) {
            rowInstall(file)
        }
        ApkInstaller.completeInstall(file)
    }

    private fun rowInstall(file: File) {
        try {
            val command = "cat ${file.path} | pm install -S ${file.length()}"
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
