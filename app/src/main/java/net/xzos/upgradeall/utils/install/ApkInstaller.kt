package net.xzos.upgradeall.utils.install

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import net.xzos.upgradeall.application.MyApplication.Companion.context
import net.xzos.upgradeall.core.oberver.ObserverFun
import net.xzos.upgradeall.data.PreferencesMap
import net.xzos.upgradeall.utils.ToastUtil
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
            else -> ApkSystemInstaller.install(file)
        }
    }

    fun observeInstall(apkFile: File, observerFun: ObserverFun<Unit>) {
        val packageInfo = apkFile.getPackageInfo() ?: return
        val key = Pair(packageInfo.packageName, packageInfo.versionName).getMapKey()
        ApkSystemInstaller.observeForever(key, observerFun)
    }

    fun completeInstall(packageNameFile: File) {
        val packageInfo = packageNameFile.getPackageInfo() ?: return
        completeInstall(packageInfo)
    }

    fun completeInstall(packageInfo: PackageInfo) {
        val key = Pair(packageInfo.packageName, packageInfo.versionName).getMapKey()
        ApkSystemInstaller.notifyChanged(key)
        ApkSystemInstaller.removeObserver(key)
    }

    private fun Pair<String, String>.getMapKey(): String {
        return "$first:$second"
    }
}

class AppInstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val manager = context.packageManager
        val packageName = intent.data!!.schemeSpecificPart

        try {
            val info = manager.getPackageInfo(packageName, 0)
            ApkInstaller.completeInstall(info)
        } catch (e: PackageManager.NameNotFoundException) {
            ToastUtil.makeText(e.toString())
        }
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
