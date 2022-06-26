package net.xzos.upgradeall.core.shell

import com.jaredrummler.ktsh.Shell


object CoreShell {

    private val session: Shell by lazy {
        Shell.SH
    }
    private val rootSession: Shell? by lazy {
        Shell.SH.apply { run("su") }
    }

    fun runShellCommand(commands: String): Shell.Command.Result? {
        return runShell(commands, session)
    }

    fun runSuShellCommand(commands: String): Shell.Command.Result? {
        return rootSession?.let { runShell(commands, it) }
    }

    private fun runShell(commands: String, session: Shell): Shell.Command.Result {
        return session.run(commands)
    }
}

/**
 * 本用于简化 Shell 操作
 * 后切换 Shell 库后为已有的功能
 * 目前用作调用统计
 */
fun Shell.Command.Result.getOutputString(): String {
    return stdout()
}

fun Shell.Command.Result.getErrorsString(): String {
    return stderr()
}