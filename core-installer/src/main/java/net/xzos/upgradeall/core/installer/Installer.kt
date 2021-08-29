package net.xzos.upgradeall.core.installer

import android.content.Context
import net.xzos.upgradeall.core.utils.oberver.ObserverFun
import net.xzos.upgradeall.core.utils.oberver.ObserverFunNoArg
import java.io.File

object Installer {
    suspend fun install(
        file: File, context: Context,
        failedInstallObserverFun: ObserverFun<Throwable>,
        completeInstallObserverFun: ObserverFunNoArg
    ) {
        if (file.installableMagiskModule())
            MagiskModuleInstaller.install(
                file, failedInstallObserverFun, completeInstallObserverFun
            )
        else ApkInstaller.install(
            file, context, failedInstallObserverFun, completeInstallObserverFun
        )
    }
}