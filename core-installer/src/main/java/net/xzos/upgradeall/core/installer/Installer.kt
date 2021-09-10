package net.xzos.upgradeall.core.installer

import android.content.Context
import net.xzos.upgradeall.core.utils.oberver.ObserverFun
import net.xzos.upgradeall.core.utils.oberver.ObserverFunNoArg
import java.io.File

object Installer {
    suspend fun install(
        fileList: List<File>, context: Context,
        failedInstallObserverFun: ObserverFun<Throwable>,
        completeInstallObserverFun: ObserverFunNoArg
    ) {
        if (fileList.size == 1) {
            val file = fileList.first()
            when {
                checkIsApk(file, context) -> ApkInstaller.install(
                    file, context, failedInstallObserverFun, completeInstallObserverFun
                )
                checkIsMagiskModule(file) -> MagiskModuleInstaller.install(
                    file, failedInstallObserverFun, completeInstallObserverFun
                )
                else -> return
            }
        } else {
            ApkInstaller.multipleInstall(
                fileList.first().parentFile!!, context,
                failedInstallObserverFun, completeInstallObserverFun
            )
        }
    }
}