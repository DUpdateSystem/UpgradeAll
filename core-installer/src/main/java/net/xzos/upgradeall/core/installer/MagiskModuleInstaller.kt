package net.xzos.upgradeall.core.installer

import net.xzos.upgradeall.core.shell.CoreShell
import net.xzos.upgradeall.core.shell.getErrorsString
import net.xzos.upgradeall.core.shell.getOutputString
import net.xzos.upgradeall.core.utils.log.Log
import net.xzos.upgradeall.core.utils.log.ObjectTag
import net.xzos.upgradeall.core.utils.log.ObjectTag.Companion.core
import net.xzos.upgradeall.core.utils.log.msg
import net.xzos.upgradeall.core.utils.oberver.Func
import net.xzos.upgradeall.core.utils.oberver.FuncNoArg
import java.io.File

internal object MagiskModuleInstaller {
    private const val TAG = "MagiskInstaller"
    private val logObjectTag = ObjectTag(core, TAG)

    suspend fun install(
        file: File,
        failedInstallObserverFun: Func<Throwable>,
        completeInstallObserverFun: FuncNoArg
    ) {
        val command = "magisk --install-module ${file.path}"
        try {
            CoreShell.runSuShellCommand(command).also {
                if (it.exitCode == 0) {
                    completeInstallObserverFun()
                } else {
                    val errorMsg =
                        "Error: out: ${it.getOutputString()}, err: ${it.getErrorsString()}"
                    Log.e(logObjectTag, TAG, errorMsg)
                    failedInstallObserverFun(RuntimeException(errorMsg))
                }
            }
        } catch (e: Throwable) {
            Log.e(logObjectTag, TAG, e.msg())
            failedInstallObserverFun(e)
            throw e
        }
    }
}