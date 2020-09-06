package net.xzos.upgradeall.utils.file

import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ZipFile {
    private val byteArrayOutputStream = ByteArrayOutputStream()
    private val zipOutputStream = ZipOutputStream(byteArrayOutputStream)

    fun getByteArray(): ByteArray {
        zipOutputStream.close()
        return byteArrayOutputStream.toByteArray()
    }

    fun zipDirectory(sourceFile: File, parentDirPath: String) {
        sourceFile.listFiles()?.forEach { f ->
            if (f.isDirectory) {
                val path = if (parentDirPath == "") {
                    f.name
                } else {
                    parentDirPath + File.separator + f.name
                }
                newDirectory(f, path)
                //Call recursively to add files within this directory
                zipDirectory(f, path)
            } else {
                zipFile(f, parentDirPath)
            }
        }
    }

    private fun newDirectory(dirFile: File, path: String) {
        val entry = ZipEntry(path + File.separator)
        entry.time = dirFile.lastModified()
        entry.size = dirFile.length()
        zipOutputStream.putNextEntry(entry)
    }

    fun zipByteFile(bytes: ByteArray, name: String, parentDirPath: String = "") {
        val path = parentDirPath + File.separator + name
        val entry = ZipEntry(path)
        writeByteToEntry(bytes, entry)
    }

    fun zipFile(file: File, parentDirPath: String = "") {
        if (!file.exists()) return
        val path = parentDirPath + File.separator + file.name
        val entry = ZipEntry(path).apply {
            time = file.lastModified()
            size = file.length()
        }
        writeByteToEntry(file.readBytes(), entry)
    }

    private fun writeByteToEntry(bytes: ByteArray, entry: ZipEntry) {
        zipOutputStream.putNextEntry(entry)
        zipOutputStream.write(bytes)
        zipOutputStream.closeEntry()
    }
}
