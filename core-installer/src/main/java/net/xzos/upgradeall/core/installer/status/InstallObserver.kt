package net.xzos.upgradeall.core.installer.status

import android.content.Context
import android.content.pm.PackageInfo
import net.xzos.upgradeall.core.installer.getPackageInfo
import net.xzos.upgradeall.core.utils.oberver.FuncNoArg
import net.xzos.upgradeall.core.utils.oberver.InformerNoArg
import net.xzos.upgradeall.core.utils.oberver.Tag
import net.xzos.upgradeall.core.utils.watchdog.WatchdogItem
import java.io.File

data class PackageInfoData(
    val packageName: String,
    val versionName: String,
) : Tag

internal object InstallObserver : InformerNoArg<(PackageInfoData)>() {

    fun observeInstall(file: File, context: Context, observerFun: FuncNoArg) {
        val key = file.getPackageInfo(context)?.observeKey() ?: return
        observe(key, observerFun = observerFun)
    }

    fun notifyComplete(packageInfo: PackageInfo) {
        val key = packageInfo.observeKey()
        notifyChanged(key)
        removeObserver(key)
    }

    fun notifyFail(file: File, context: Context) {
        val key = file.getPackageInfo(context)?.observeKey() ?: return
        notifyChanged(key)
        removeObserver(key)
    }

    private fun PackageInfo.observeKey() = PackageInfoData(packageName, versionName)
}