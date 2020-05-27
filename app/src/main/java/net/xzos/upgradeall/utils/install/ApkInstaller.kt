package net.xzos.upgradeall.utils.install

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import net.xzos.upgradeall.application.MyApplication.Companion.context
import net.xzos.upgradeall.core.oberver.Observer
import net.xzos.upgradeall.data.PreferencesMap
import java.io.File

object ApkInstaller {

    init {
        AppInstallReceiver().register()
    }

    suspend fun install(file: File) {
        if (!file.isApkFile()) return
        when (PreferencesMap.install_apk_api) {
            "System" -> ApkSystemInstaller.install(file)
            "Root" -> ApkRootInstall.install(file)
            "Shizuku" -> ApkShizukuInstaller.install(file)
        }
    }

    fun observeForever(apkFile: File, observer: Observer) {
        val packageInfo = apkFile.getPackageInfo() ?: return
        ApkSystemInstaller.observeForever(
                Pair(packageInfo.packageName, packageInfo.versionName).getMapKey(),
                observer)
    }

    fun completeInstall(packageName: String, versionName: String) {
        val key = Pair(packageName, versionName).getMapKey()
        ApkSystemInstaller.notifyChanged(tag = key)
    }

    private fun Pair<String, String>.getMapKey(): String {
        return "$first:$second"
    }
}

class AppInstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val manager = context.packageManager
        val packageName = intent.data!!.schemeSpecificPart
        val info = manager.getPackageInfo(packageName, 0)
        ApkInstaller.completeInstall(info.packageName, info.versionName)
    }


    fun register() {
        val intentFilter = IntentFilter()
        intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED)
        intentFilter.addAction(Intent.ACTION_PACKAGE_REPLACED)
        intentFilter.addDataScheme("package")
        context.registerReceiver(this, intentFilter)
    }
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
