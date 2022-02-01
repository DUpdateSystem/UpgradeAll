package net.xzos.upgradeall.core.shell

import eu.darken.rxshell.cmd.Cmd
import eu.darken.rxshell.cmd.RxCmdShell
import eu.darken.rxshell.root.Root

object Shell {

    private val session by lazy {
        RxCmdShell.builder().build().open().blockingGet()
    }
    private val rootSession by lazy {
        if (Root.Builder().build().blockingGet().state == Root.State.ROOTED)
            RxCmdShell.builder().root(true).build().open().blockingGet()
        else null
    }

    fun runShellCommand(commands: String): Cmd.Result? {
        return runShell(commands, session)
    }

    fun runSuShellCommand(commands: String): Cmd.Result? {
        return runShell(commands, rootSession ?: return null)
    }

    private fun runShell(commands: String, session: RxCmdShell.Session): Cmd.Result? {
        return Cmd.builder(commands).execute(session)
    }
}

fun Cmd.Result.getOutputString(): String {
    val output = this.output
    var outputString = ""
    for (out in output)
        outputString += "$out\n"
    return outputString
}

fun Cmd.Result.getErrorsString(): String {
    val errors = this.errors
    var errorString = ""
    for (out in errors)
        errorString += "$out\n"
    return errorString
}