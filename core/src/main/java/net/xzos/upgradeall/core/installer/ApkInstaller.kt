package net.xzos.upgradeall.core.installer

import android.content.pm.PackageInfo
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.xzos.upgradeall.core.coreConfig
import net.xzos.upgradeall.core.utils.oberver.ObserverFun
import net.xzos.upgradeall.core.utils.oberver.ObserverFunNoArg
import java.io.File


object ApkInstaller {

    private val installMutex = Mutex()

    init {
        AppInstallReceiver().register()
    }

    suspend fun install(
            file: File,
            failedInstallObserverFun: ObserverFun<Throwable>,
            completeInstallObserverFun: ObserverFunNoArg
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
            completeInstallObserverFun: ObserverFunNoArg
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

    private fun observeInstall(apkFile: File, observerFun: ObserverFunNoArg) {
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