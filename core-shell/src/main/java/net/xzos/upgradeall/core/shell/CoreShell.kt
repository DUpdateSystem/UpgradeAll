package net.xzos.upgradeall.core.shell


object CoreShell {

    fun runShellCommand(commands: String): ShellResult {
        return try {
            Shell().run("sh", commands)
        } catch (e: Throwable) {
            ShellResult()
        }
    }

    fun runSuShellCommand(commands: String): ShellResult {
        return try {
            Shell().run("su", commands)
        } catch (e: Throwable) {
            ShellResult()
        }
    }
}

/**
 * 本用于简化 Shell 操作
 * 后切换 Shell 库后为已有的功能
 * 目前用作调用统计
 */
fun ShellResult.getOutputString(): String {
    return stdout()
}

fun ShellResult.getErrorsString(): String {
    return stderr()
}