package net.xzos.upgradeall.core.installer

import android.content.Context
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.xzos.upgradeall.core.androidutils.getCurrentUserId
import net.xzos.upgradeall.core.installer.installerapi.ApkRootInstall
import net.xzos.upgradeall.core.installer.installerapi.ApkShizukuInstaller
import net.xzos.upgradeall.core.installer.installerapi.ApkSystemInstaller
import net.xzos.upgradeall.core.installer.status.InstallObserver
import net.xzos.upgradeall.core.utils.oberver.Func
import net.xzos.upgradeall.core.utils.oberver.FuncNoArg
import java.io.File


internal object ApkInstaller {

    private val installMutex = Mutex()

    var installMode = "System"

    suspend fun install(
        file: File, context: Context,
        failedInstallObserverFun: Func<Throwable>,
        completeInstallObserverFun: FuncNoArg
    ) {
        installMutex.withLock {
            try {
                InstallObserver.observeInstall(file, context, completeInstallObserverFun)
                doInstall(file, context)
            } catch (e: Throwable) {
                InstallObserver.notifyFail(file, context)
                failedInstallObserverFun(e)
            }
        }
    }

    private suspend fun doInstall(
        file: File, context: Context
    ) {
        when (installMode) {
            "System" -> ApkSystemInstaller.install(file, context)
            "Root" -> ApkRootInstall.install(file, userId = getCurrentUserId(context))
            "Shizuku" -> ApkShizukuInstaller.install(file, context)
            else -> ApkSystemInstaller.install(file, context)
        }
    }

    suspend fun multipleInstall(
        dirFile: File, context: Context,
        failedInstallObserverFun: Func<Throwable>,
        completeInstallObserverFun: FuncNoArg
    ) {
        installMutex.withLock {
            try {
                InstallObserver.observeInstall(dirFile, context, completeInstallObserverFun)
                doMultipleInstall(dirFile, context)
            } catch (e: Throwable) {
                InstallObserver.notifyFail(dirFile, context)
                failedInstallObserverFun(e)
            }
        }
    }

    @Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    private suspend fun doMultipleInstall(
        dirFile: File, context: Context
    ) {
        val apkFilePathList = dirFile.listFiles().filter {
            it.extension == "apk"
        }
        when (installMode) {
            "System" -> ApkSystemInstaller.multipleInstall(apkFilePathList, context)
            "Root" -> ApkRootInstall.multipleInstall(
                apkFilePathList,
                userId = getCurrentUserId(context)
            )

            "Shizuku" -> ApkShizukuInstaller.multipleInstall(apkFilePathList, context)
            else -> ApkSystemInstaller.multipleInstall(apkFilePathList, context)
        }
        val obbFilePathList = dirFile.listFiles().filter {
            it.extension == "obb"
        }
        when (installMode) {
            "System" -> ApkSystemInstaller.obbInstall(obbFilePathList)
            "Root" -> ApkRootInstall.obbInstall(
                obbFilePathList, userId = getCurrentUserId(context)
            )

            else -> ApkSystemInstaller.obbInstall(obbFilePathList)
        }
    }
}