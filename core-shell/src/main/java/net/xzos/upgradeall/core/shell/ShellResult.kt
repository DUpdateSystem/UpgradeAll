package net.xzos.upgradeall.core.shell

data class ShellResult(
    val stdout: MutableList<String> = mutableListOf(),
    val stderr: MutableList<String> = mutableListOf(),
    var exitCode: Int = -1,
) {
    fun stdout() = stdout.joinToString("\n")
    fun stderr() = stderr.joinToString("\n")
}