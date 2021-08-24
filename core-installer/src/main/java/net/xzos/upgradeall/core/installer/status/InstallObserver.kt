package net.xzos.upgradeall.core.installer.status

import android.content.Context
import android.content.pm.PackageInfo
import net.xzos.upgradeall.core.installer.getPackageInfo
import net.xzos.upgradeall.core.installer.installerapi.ApkSystemInstaller
import net.xzos.upgradeall.core.utils.coroutines.coroutinesMutableMapOf
import net.xzos.upgradeall.core.utils.oberver.Informer
import net.xzos.upgradeall.core.utils.oberver.ObserverFunNoArg
import net.xzos.upgradeall.core.utils.oberver.Tag
import net.xzos.upgradeall.core.utils.watchdog.WatchdogItem
import java.io.File

private data class PackageInfoTag(
    val packageName: String,
    val versionName: String,
) : Tag

internal object InstallObserver : Informer {
    override val informerId = Informer.getInformerId()

    private val filePackageMap = coroutinesMutableMapOf<File, PackageInfo>(true)

    fun observeInstall(apkFile: File, context: Context, observerFun: ObserverFunNoArg) {
        val packageInfo = apkFile.getPackageInfo(context)
        if (packageInfo != null) {
            filePackageMap[apkFile] = packageInfo
            val key = getObserveKey(packageInfo)
            ApkSystemInstaller.observeForever(key, observerFun)
        } else {
            val watchdog = WatchdogItem(30).apply {
                addStopListener(observerFun)
            }
            watchdog.start()
        }
    }

    fun notifyCompleteInstall(packageInfo: PackageInfo) {
        val key = getObserveKey(packageInfo)
        ApkSystemInstaller.notifyChanged(key)
        ApkSystemInstaller.removeObserver(key)
    }

    fun removeObserver(file: File) {
        val packageInfo = filePackageMap.remove(file) ?: return
        val key = getObserveKey(packageInfo)
        ApkSystemInstaller.removeObserver(key)
    }

    private fun getObserveKey(packageInfo: PackageInfo): PackageInfoTag {
        return PackageInfoTag(packageInfo.packageName, packageInfo.versionName)
    }
}