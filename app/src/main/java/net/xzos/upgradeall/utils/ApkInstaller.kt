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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import net.xzos.upgradeall.BuildConfig
import net.xzos.upgradeall.application.MyApplication
import net.xzos.upgradeall.application.MyApplication.Companion.context
import net.xzos.upgradeall.core.data_manager.utils.wait
import net.xzos.upgradeall.data.PreferencesMap
import java.io.File


object ApkInstaller {

    private val context: Context = MyApplication.context
    private val installMutexMap = mutableMapOf<String, Mutex>()

    suspend fun install(file: File) {
        withContext(Dispatchers.Default) {
            rowInstall(file)
        }
    }

    private suspend fun rowInstall(file: File) {
        // 修复后缀名
        if (!file.isApkFile()) return
        val apkFile = file.autoAddApkExtension()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                val m = StrictMode::class.java.getMethod("disableDeathOnFileUriExposure")
                m.invoke(null)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        val fileUri = apkFile.getUri()
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

        val mutex = Mutex(locked = true)
        getPackageInfoFromApkFile(apkFile)?.run {
            val key = Pair(this.packageName, this.versionName).getMapKey()
            installMutexMap[key] = mutex
        }
        context.startActivity(intent)
        mutex.wait()
    }

    fun completeInstall(packageName: String, versionName: String) {
        val key = Pair(packageName, versionName).getMapKey()
        val mutex = installMutexMap[key] ?: return
        if (mutex.isLocked) mutex.unlock()
    }

    private fun File.getUri(): Uri {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".fileprovider", this)
        else Uri.fromFile(this)
    }

    private fun Pair<String, String>.getMapKey(): String {
        return "$first:$second"
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


fun File.isApkFile(): Boolean {
    return getPackageInfoFromApkFile(this) != null
}

private fun getPackageInfoFromApkFile(file: File): PackageInfo? {
    return try {
        context.packageManager.getPackageArchiveInfo(file.path, PackageManager.GET_ACTIVITIES)
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
