package net.xzos.upgradeall.utils.install

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.shizuku.api.ShizukuService
import net.xzos.upgradeall.core.data.json.nongson.ObjectTag
import net.xzos.upgradeall.core.log.Log
import net.xzos.upgradeall.utils.MiscellaneousUtils
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
        if (!ShizukuService.pingBinder()) {
            return
        } else try {
            val command = "cat ${file.path} | pm install -S ${file.length()}"
            MiscellaneousUtils.runShellCommand(command, su = true)?.also {
                if (!it.isSuccessful)
                    Log.e(logObjectTag, TAG, """
                    Error: out: ${it.stdout}, err: ${it.stderr}
                """.trimIndent())
            }
        } catch (e: Throwable) {
            Log.e(logObjectTag, TAG, e.toString())
        }
    }
}
