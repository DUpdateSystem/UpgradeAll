package net.xzos.upgradeall.utils.install

import android.app.Activity
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.shizuku.api.ShizukuApiConstants
import moe.shizuku.api.ShizukuService
import net.xzos.upgradeall.R
import net.xzos.upgradeall.core.data.json.nongson.ObjectTag
import net.xzos.upgradeall.core.log.Log
import net.xzos.upgradeall.utils.FileUtil
import net.xzos.upgradeall.utils.FileUtil.SHELL_SCRIPT_CACHE_FILE
import net.xzos.upgradeall.utils.MiscellaneousUtils
import java.io.File


object ApkShizukuInstaller {

    private const val TAG = "ApkShizukuInstaller"
    private val logObjectTag = ObjectTag(ObjectTag.core, TAG)

    suspend fun install(file: File) {
        if (!file.isApkFile()) return
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
            exec(command)
        } catch (e: Throwable) {
            Log.e(logObjectTag, TAG, e.toString())
        }
    }

    fun requestShizukuPermission(activity: Activity, PERMISSIONS_REQUEST_CONTACTS: Int): Boolean {
        return MiscellaneousUtils.requestPermission(
                activity, ShizukuApiConstants.PERMISSION,
                PERMISSIONS_REQUEST_CONTACTS, R.string.shizuku_permission_request)
    }

    private fun exec(command: String): Int? {
        FileUtil.initDir(SHELL_SCRIPT_CACHE_FILE.parentFile!!)
        SHELL_SCRIPT_CACHE_FILE.writeText(command)
        return execInternal("sh", SHELL_SCRIPT_CACHE_FILE.path)
    }

    private fun execInternal(vararg command: String): Int? {
        val process = ShizukuService.newProcess(command, null, null)
        process.waitFor()
        val errorString = process.errorStream.bufferedReader().use { it.readText() }
        val exitValue = process.exitValue()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            process.destroyForcibly()
        } else {
            process.destroy()
        }
        return exitValue.also {
            if(exitValue != 0)
                Log.e(logObjectTag, TAG, """
                    Error: $errorString
                """.trimIndent())
        }
    }
}
