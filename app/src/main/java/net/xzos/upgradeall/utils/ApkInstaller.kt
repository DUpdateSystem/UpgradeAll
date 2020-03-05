package net.xzos.upgradeall.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.StrictMode
import androidx.core.content.FileProvider
import net.xzos.upgradeall.BuildConfig
import java.io.File


class ApkInstaller(private val context: Context) {

    fun autoRenameFile(file: File): File {
        var apkFile = file
        if (apkFile.extension != "apk") {
            apkFile = File(file.parent, file.name + ".apk")
            file.renameTo(apkFile)
        }
        return apkFile
    }

    fun installApplication(file: File) {
        // 修复后缀名
        val apkFile = autoRenameFile(file)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                val m = StrictMode::class.java.getMethod("disableDeathOnFileUriExposure")
                m.invoke(null)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        val fileUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            FileProvider.getUriForFile(context, context.packageName + ".fileprovider", apkFile)
        else Uri.fromFile(apkFile)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls())
                return
        }
        val intent = Intent(Intent.ACTION_VIEW)
                .setDataAndType(fileUri, "application/vnd.android.package-archive")
                .putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                .apply {
                    this.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        this.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                }
        context.startActivity(intent)
    }

    private fun uriFromFile(file: File): Uri {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", file)
        } else {
            Uri.fromFile(file)
        }
    }

    fun isApkFile(file: File): Boolean {
        return try {
            val pm = context.packageManager
            val info = pm.getPackageArchiveInfo(file.path, PackageManager.GET_ACTIVITIES)
            info != null
        } catch (e: Exception) {
            false
        }
    }
}