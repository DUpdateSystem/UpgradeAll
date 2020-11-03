package net.xzos.upgradeall.utils.install

import android.net.Uri
import android.os.Build
import android.os.StrictMode
import net.xzos.upgradeall.utils.file.getProviderUri
import java.io.File

@Throws(IllegalArgumentException::class)
fun File.getApkUri(): Uri {
    // 修复后缀名
    val apkFile = this.autoAddApkExtension()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        try {
            val m = StrictMode::class.java.getMethod("disableDeathOnFileUriExposure")
            m.invoke(null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    return try {
        apkFile.getProviderUri()
    } catch (e: IllegalArgumentException) {
        throw e
    }
}

// 修复后缀名
fun File.autoAddApkExtension(): File {
    if (this.isApkFile()) {
        if (this.extension != "apk") {
            val newFile = File(parent, "$name.apk")
            return if (this.renameTo(newFile))
                newFile
            else this
        }
    }
    return this
}
