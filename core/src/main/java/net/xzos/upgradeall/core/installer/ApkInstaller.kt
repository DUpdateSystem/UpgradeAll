package net.xzos.upgradeall.core.installer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.xzos.upgradeall.core.coreConfig
import net.xzos.upgradeall.core.log.Log
import net.xzos.upgradeall.core.log.ObjectTag
import net.xzos.upgradeall.core.log.ObjectTag.Companion.core
import net.xzos.upgradeall.core.log.msg
import net.xzos.upgradeall.core.utils.oberver.ObserverFun
import java.io.File

object ApkInstaller {

    private val installMutex = Mutex()

    init {
        AppInstallReceiver().register()
    }

    suspend fun install(
        file: File,
        failedInstallObserverFun: ObserverFun<Throwable>,
        completeInstallObserverFun: ObserverFun<Unit>
    ) {
        installMutex.withLock {
            try {
                observeInstall(file, completeInstallObserverFun)
                doInstall(file)
            } catch (e: Throwable) {
                removeObserver(file)
                failedInstallObserverFun(e)
            }
        }
    }

    private suspend fun doInstall(
        file: File,
    ) {
        when (coreConfig.install_apk_api) {
            "System" -> ApkSystemInstaller.install(file)
            "Root" -> ApkRootInstall.install(file)
            "Shizuku" -> ApkShizukuInstaller.install(file)
            else -> ApkSystemInstaller.install(file)
        }
    }

    suspend fun multipleInstall(
        dirFile: File,
        failedInstallObserverFun: ObserverFun<Throwable>,
        completeInstallObserverFun: ObserverFun<Unit>
    ) {
        installMutex.withLock {
            try {
                observeInstall(dirFile, completeInstallObserverFun)
                doMultipleInstall(dirFile)
            } catch (e: Throwable) {
                removeObserver(dirFile)
                failedInstallObserverFun(e)
            }
        }
    }

    @Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    private suspend fun doMultipleInstall(
        dirFile: File,
    ) {
        val apkFilePathList = dirFile.listFiles().filter {
            it.extension == "apk"
        }
        when (coreConfig.install_apk_api) {
            "System" -> ApkSystemInstaller.multipleInstall(apkFilePathList)
            "Root" -> ApkRootInstall.multipleInstall(apkFilePathList)
            "Shizuku" -> ApkShizukuInstaller.multipleInstall(apkFilePathList)
            else -> ApkSystemInstaller.multipleInstall(apkFilePathList)
        }
        val obbFilePathList = dirFile.listFiles().filter {
            it.extension == "obb"
        }
        when (coreConfig.install_apk_api) {
            "System" -> ApkSystemInstaller.obbInstall(obbFilePathList)
            "Root" -> ApkRootInstall.obbInstall(obbFilePathList)
            else -> ApkSystemInstaller.obbInstall(obbFilePathList)
        }
    }

    private fun observeInstall(apkFile: File, observerFun: ObserverFun<Unit>) {
        val packageInfo = apkFile.getPackageInfo() ?: return
        val key = Pair(packageInfo.packageName, packageInfo.versionName).getMapKey()
        ApkSystemInstaller.observeForever(key, observerFun)
    }

    internal fun notifyCompleteInstall(packageInfo: PackageInfo) {
        val key = Pair(packageInfo.packageName, packageInfo.versionName).getMapKey()
        ApkSystemInstaller.notifyChanged(key)
        ApkSystemInstaller.removeObserver(key)
    }

    private fun removeObserver(file: File) {
        val packageInfo = file.getPackageInfo() ?: return
        val key = Pair(packageInfo.packageName, packageInfo.versionName).getMapKey()
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
            ApkInstaller.notifyCompleteInstall(info)
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(logObjectTag, TAG, "error: ${e.msg()}")
        }
    }


    fun register() {
        val intentFilter = IntentFilter()
        intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED)
        intentFilter.addAction(Intent.ACTION_PACKAGE_REPLACED)
        intentFilter.addDataScheme("package")
        coreConfig.androidContext.registerReceiver(this, intentFilter)
    }

    companion object {
        private const val TAG = "AppInstallReceiver"
        private val logObjectTag = ObjectTag(core, TAG)
    }
}

fun File.isApkFile(): Boolean {
    return this.getPackageInfo() != null
}

fun File.getPackageInfo(): PackageInfo? {
    return try {
        coreConfig.androidContext.packageManager.getPackageArchiveInfo(
            this.path,
            PackageManager.GET_ACTIVITIES
        )
    } catch (e: Exception) {
        null
    }
}