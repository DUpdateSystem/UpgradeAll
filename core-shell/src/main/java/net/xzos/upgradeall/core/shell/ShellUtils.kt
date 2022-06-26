package net.xzos.upgradeall.core.shell

fun getFileNameList(folderPath: String): List<String> {
    val folderPathString = folderPath.apply {
        val separator = '/'
        if (this.last() != separator)
            this.plus(separator)
    }
    val command = """ for entry in "${'$'}search_dir"${folderPathString}*
            do
              echo "${'$'}entry"
            done """.trimIndent()

    val result = CoreShell.runSuShellCommand(command)
    return result.getOutputString()
        .split("\n".toRegex())
        .dropLastWhile { it.isEmpty() }
        .map { it.removePrefix(folderPathString) }
}

fun getFileText(filePath: String): String {
    val command = "cat $filePath"
    return CoreShell.runSuShellCommand(command).getOutputString()
}