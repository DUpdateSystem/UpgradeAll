package net.xzos.upgradeall.core.shell

import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader

class Shell {
    fun run(shell: String, vararg command: String): ShellResult {
        val proc = Runtime.getRuntime().exec(shell)
        val stdin = DataOutputStream(proc.outputStream)
        command.forEach {
            stdin.write("$it\n".toByteArray(Charsets.UTF_8))
        }
        stdin.write("exit\n".toByteArray(Charsets.UTF_8))
        stdin.flush()
        val stdInput = BufferedReader(InputStreamReader(proc.inputStream))
        val stdError = BufferedReader(InputStreamReader(proc.errorStream))
        val result = ShellResult()
        while (true) {
            stdInput.readLine()?.also {
                result.stdout.add(it)
            } ?: break
        }
        while (true) {
            stdError.readLine()?.also {
                result.stderr.add(it)
            } ?: break
        }
        stdInput.close()
        stdError.close()
        proc.waitFor()
        result.exitCode = proc.exitValue()
        return result
    }
}