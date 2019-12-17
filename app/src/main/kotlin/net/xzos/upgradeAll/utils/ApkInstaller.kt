package net.xzos.upgradeAll.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.StrictMode
import androidx.core.content.FileProvider
import net.xzos.upgradeAll.BuildConfig
import java.io.File


class ApkInstaller(private val context: Context) {

    fun installApplication(file: File) {
        if (Build.VERSION.SDK_INT >= 24) {
            try {
                val m = StrictMode::class.java.getMethod("disableDeathOnFileUriExposure")
                m.invoke(null)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        var fileUri = Uri.fromFile(file)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            fileUri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
        val intent = Intent(Intent.ACTION_INSTALL_PACKAGE)
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