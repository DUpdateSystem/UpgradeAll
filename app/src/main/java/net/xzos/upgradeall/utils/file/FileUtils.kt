package net.xzos.upgradeall.utils.file

import java.io.File

val String.fileName: String
    get() = this.substringAfterLast('/', "")

fun File.deleteChildRecursive() {
    if (this.isDirectory) {
        listFiles().forEach {
            it.deleteRecursively()
        }
    }
}