package net.xzos.upgradeAll.utils

import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
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
        context.startActivity(
                Intent(Intent.ACTION_INSTALL_PACKAGE)
                        .setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive")
                        .apply {
                            this.flags = FLAG_ACTIVITY_NEW_TASK
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                this.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                        }
        )
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