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

fun File.installable(): Boolean {
    return this.installableMagiskModule() || this.installableApk()
}

fun File.installableMagiskModule(): Boolean {
    var installable = false
    parseZipBytes(this.readBytes()) { name, bytes ->
        return@parseZipBytes if (name == "module.prop"){
            installable = true
            true
        }
        else false
    }
    return installable
}
fun File.installableApk(): Boolean {
    return if (this.isDirectory) {
        for (file in this.listFiles() ?: return false) {
            if (file.extension == "apk")
                return true
        }
        false
    } else {
        extension == "apk"
    }
}

fun DocumentFile.installable(): Boolean {
    return if (this.isDirectory) {
        for (file in this.listFiles()) {
            if (file.extension == "apk")
                return true
        }
        false
    } else {
        extension == "apk"
    }
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
