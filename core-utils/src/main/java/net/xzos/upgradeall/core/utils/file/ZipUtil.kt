package net.xzos.upgradeall.core.utils.file

import android.os.Build
import dalvik.system.ZipPathValidator
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipInputStream
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
        val path = (parentDirPath + File.separator + name).removePrefix("/")
        val entry = ZipEntry(path)
        writeByteToEntry(bytes, entry)
    }

    fun zipFile(file: File, parentDirPath: String = "") {
        if (!file.exists()) return
        val path = (parentDirPath + File.separator + file.name).removePrefix("/")
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

fun parseZipBytes(
    zipFileByteArray: ByteArray,
    allowedPaths: Set<String>? = null,
    callback: (String, ByteArray) -> Boolean,
) {
    // Set up custom ZipPathValidator for Android 14+ (API 34+) to support legacy backup files
    // This allows zip entries with absolute paths (starting with "/") that Android 16 normally rejects
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        ZipPathValidator.setCallback(object : ZipPathValidator.Callback {
            override fun onZipEntryAccess(path: String) {
                // If allowedPaths is provided, check whitelist; otherwise allow all
                val normalizedPath = path.removePrefix("/")
                if (allowedPaths != null && !allowedPaths.contains(normalizedPath)) {
                    throw ZipException("Invalid zip entry path not in whitelist: $path")
                }
            }
        })
    }

    try {
        ZipInputStream(zipFileByteArray.inputStream()).use { zis ->
            var ze: ZipEntry?
            var count: Int
            val buffer = ByteArray(8192)
            while (zis.nextEntry.also { ze = it } != null) {
                // Normalize entry name by removing leading slashes for consistent matching
                val name = ze!!.name.removePrefix("/")
                val byteArrayOutputStream = ByteArrayOutputStream()
                byteArrayOutputStream.use { it ->
                    while (zis.read(buffer).also { count = it } != -1) it.write(buffer, 0, count)
                }
                val bytes = byteArrayOutputStream.toByteArray()
                if (callback(name, bytes)) break
            }
        }
    } finally {
        // Clean up custom validator to avoid affecting other zip operations
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ZipPathValidator.clearCallback()
        }
    }
}

