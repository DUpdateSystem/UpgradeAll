package net.xzos.upgradeall.core.installer

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.StrictMode
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import net.xzos.upgradeall.core.utils.file.parseZipBytes
import java.io.File

@Throws(IllegalArgumentException::class)
fun File.getProviderUri(context: Context): Uri {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
        try {
            FileProvider.getUriForFile(
                context, context.packageName + ".fileprovider",
                this
            )
        } catch (e: IllegalArgumentException) {
            throw e
        }
    else Uri.fromFile(this)
}

@Throws(IllegalArgumentException::class)
fun File.getApkUri(context: Context): Uri {
    // 修复后缀名
    val apkFile = this.autoAddApkExtension(context)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        try {
            val m = StrictMode::class.java.getMethod("disableDeathOnFileUriExposure")
            m.invoke(null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    return try {
        apkFile.getProviderUri(context)
    } catch (e: IllegalArgumentException) {
        throw e
    }
}

// 修复后缀名
fun File.autoAddApkExtension(context: Context): File {
    if (this.isApkFile(context)) {
        if (this.extension != "apk") {
            val newFile = File(parent, "$name.apk")
            return if (this.renameTo(newFile))
                newFile
            else this
        }
    }
    return this
}

fun getFileType(fileList: List<File>, context: Context): FileType? {
    if (fileList.size == 1) {
        val file = fileList.first()
        if (checkIsApk(file, context))
            return FileType.APK
        else if (checkIsMagiskModule(file))
            return FileType.MAGISK_MODULE
    } else {
        if (checkIsApk(fileList, context))
            return FileType.APK
    }
    return null
}

fun checkIsApk(fileList: List<File>, context: Context): Boolean {
    fileList.forEach {
        if (it.extension == "apk")
            return true
    }
    return fileList.first().parentFile?.getPackageInfo(context) != null
}

fun checkIsApk(file: File, context: Context): Boolean {
    return file.extension == "apk" || file.getPackageInfo(context) != null
}

fun checkIsMagiskModule(file: File): Boolean {
    return if (file.extension == "zip") {
        var installable = false
        parseZipBytes(file.readBytes()) { name, _ ->
            return@parseZipBytes if (name == "module.prop") {
                installable = true
                true
            } else false
        }
        installable
    } else false
}

val DocumentFile.extension: String
    get() = name?.substringAfterLast('.', "") ?: ""

fun File.isApkFile(context: Context): Boolean {
    return this.getPackageInfo(context) != null
}

fun File.getPackageInfo(context: Context): PackageInfo? {
    return try {
        context.packageManager.getPackageArchiveInfo(this.path, PackageManager.GET_ACTIVITIES)
    } catch (e: Exception) {
        null
    }
}
