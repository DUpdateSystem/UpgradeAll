package net.xzos.upgradeall.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.StrictMode
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.xzos.upgradeall.BuildConfig
import net.xzos.upgradeall.application.MyApplication
import net.xzos.upgradeall.application.MyApplication.Companion.context
import net.xzos.upgradeall.core.oberver.Informer
import net.xzos.upgradeall.core.oberver.Observer
import net.xzos.upgradeall.data.PreferencesMap
import java.io.File


object ApkInstaller : Informer() {

    private val context: Context = MyApplication.context

    suspend fun install(file: File) {
        // 修复后缀名
        if (!file.isApkFile()) return
        withContext(Dispatchers.Default) {
            val fileUri = file.getApkUri()
            rowInstall(fileUri)
        }
    }

    private fun rowInstall(fileUri: Uri) {
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

    fun completeInstall(packageName: String, versionName: String) {
        val key = Pair(packageName, versionName).getMapKey()
        notifyChanged(tag = key)
    }

    private fun Pair<String, String>.getMapKey(): String {
        return "$first:$second"
    }

    fun observeForever(apkFile: File, observer: Observer) {
        val packageInfo = apkFile.getPackageInfo() ?: return
        observeForever(
                Pair(packageInfo.packageName, packageInfo.versionName).getMapKey(),
                observer)
    }
}

fun File.autoAddApkExtension(): File {
    if (this.isApkFile()) {
        if (this.extension != "apk") {
            this.renameTo(File(parent, "$name.apk"))
        }
    }
    return this
}

fun File.getApkUri(): Uri {
    val apkFile = this.autoAddApkExtension()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        try {
            val m = StrictMode::class.java.getMethod("disableDeathOnFileUriExposure")
            m.invoke(null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    return apkFile.getUri()
}

fun File.getUri(): Uri {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
        FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".fileprovider", this)
    else Uri.fromFile(this)
}

fun File.isApkFile(): Boolean {
    return this.getPackageInfo() != null
}


fun File.getPackageInfo(): PackageInfo? {
    return try {
        context.packageManager.getPackageArchiveInfo(this.path, PackageManager.GET_ACTIVITIES)
    } catch (e: Exception) {
        null
    }
}

class AppInstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val manager = context.packageManager
        if (intent.action == Intent.ACTION_PACKAGE_REPLACED && PreferencesMap.auto_delete_file) {
            val packageName = intent.data!!.schemeSpecificPart
            val info = manager.getPackageInfo(packageName, 0)
            ApkInstaller.completeInstall(info.packageName, info.versionName)
        }
    }
}
