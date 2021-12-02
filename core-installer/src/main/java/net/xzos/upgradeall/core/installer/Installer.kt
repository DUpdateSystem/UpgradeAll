package net.xzos.upgradeall.core.installer

import android.content.Context
import net.xzos.upgradeall.core.utils.oberver.ObserverFun
import net.xzos.upgradeall.core.utils.oberver.ObserverFunNoArg
import java.io.File

object Installer {
    suspend fun install(
        fileList: List<File>,
        fileType: FileType, context: Context,
        failedInstallObserverFun: ObserverFun<Throwable>,
        completeInstallObserverFun: ObserverFunNoArg
    ) {
        if (fileList.size == 1) {
            val file = fileList.first()
            when (fileType) {
                FileType.APK -> ApkInstaller.install(
                    file, context, failedInstallObserverFun, completeInstallObserverFun
                )
                FileType.MAGISK_MODULE -> MagiskModuleInstaller.install(
                    file, failedInstallObserverFun, completeInstallObserverFun
                )
            }
        } else {
            if (fileType == FileType.APK)
                ApkInstaller.multipleInstall(
                    fileList.first().parentFile!!, context,
                    failedInstallObserverFun, completeInstallObserverFun
                )
        }
    }
}