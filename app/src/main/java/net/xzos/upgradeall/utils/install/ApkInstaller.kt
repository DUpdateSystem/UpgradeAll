package net.xzos.upgradeall.utils.install

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.xzos.upgradeall.application.MyApplication.Companion.context
import net.xzos.upgradeall.core.oberver.ObserverFun
import net.xzos.upgradeall.data.PreferencesMap
import net.xzos.upgradeall.utils.ToastUtil
import java.io.File

object ApkInstaller {

    private val installMutex = Mutex()

    init {
        AppInstallReceiver().register()
    }

    suspend fun install(file: File, observerFun: ObserverFun<Unit>) {
        if (!file.isApkFile()) return
        installMutex.withLock {
            observeInstall(file, observerFun)
            when (PreferencesMap.install_apk_api) {
                "System" -> ApkSystemInstaller.install(file)
                "Root" -> ApkRootInstall.install(file)
                "Shizuku" -> ApkShizukuInstaller.install(file)
                else -> ApkSystemInstaller.install(file)
            }
        }
    }

    suspend fun multipleInstall(filePathList: List<File>, observerFun: ObserverFun<Unit>) {
        val apkFilePathList = filePathList.filter {
            it.extension == "apk"
        }
        ApkRootInstall.multipleInstall(apkFilePathList)
        val obbFilePathList = filePathList.filter {
            it.extension == "obb"
        }
        ApkRootInstall.obbInstall(obbFilePathList)
        observerFun(Unit)
    }

    private fun observeInstall(key: String, observerFun: ObserverFun<Unit>) {
        ApkSystemInstaller.observeForever(key, observerFun)
    }

    private fun observeInstall(apkFile: File, observerFun: ObserverFun<Unit>) {
        val packageInfo = apkFile.getPackageInfo() ?: return
        val key = Pair(packageInfo.packageName, packageInfo.versionName).getMapKey()
        observeInstall(key, observerFun)
    }

    fun completeInstall(packageNameFile: File) {
        val packageInfo = packageNameFile.getPackageInfo() ?: return
        completeInstall(packageInfo)
    }

    fun completeInstall(packageInfo: PackageInfo) {
        val key = Pair(packageInfo.packageName, packageInfo.versionName).getMapKey()
        completeInstall(key)
    }

    fun completeInstall(key: String) {
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
