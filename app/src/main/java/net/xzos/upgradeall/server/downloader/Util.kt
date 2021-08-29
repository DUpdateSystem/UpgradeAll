package net.xzos.upgradeall.server.downloader

import android.content.Context
import net.xzos.upgradeall.R
import net.xzos.upgradeall.core.androidutils.ToastUtil
import net.xzos.upgradeall.core.downloader.filedownloader.DownloadFetchError
import net.xzos.upgradeall.core.downloader.filedownloader.item.DownloadInfoItem
import net.xzos.upgradeall.core.downloader.filedownloader.observe.DownloadOb
import net.xzos.upgradeall.core.downloader.filetasker.FileTasker
import net.xzos.upgradeall.core.downloader.filetasker.FileTaskerId
import net.xzos.upgradeall.core.installer.Installer
import net.xzos.upgradeall.core.installer.installable
import net.xzos.upgradeall.core.module.app.App
import net.xzos.upgradeall.core.module.app.version_item.FileAsset
import net.xzos.upgradeall.core.serverApi
import net.xzos.upgradeall.core.utils.log.msg
import net.xzos.upgradeall.core.utils.oberver.ObserverFun
import net.xzos.upgradeall.core.utils.oberver.ObserverFunNoArg
import net.xzos.upgradeall.core.websdk.json.DownloadItem
import net.xzos.upgradeall.data.PreferencesMap
import net.xzos.upgradeall.utils.DOWNLOAD_EXTRA_CACHE_DIR
import net.xzos.upgradeall.utils.MiscellaneousUtils

suspend fun startDownload(
    app: App, fileAsset: FileAsset,
    taskStartedFun: (Int) -> Unit, taskStartFailedFun: (Throwable) -> Unit, downloadOb: DownloadOb,
    context: Context, externalDownload: Boolean,
) {
    ToastUtil.showText(context, R.string.download_loading)
    val hub = fileAsset.hub
    val downloadInfoList =
        serverApi.getDownloadInfo(hub.uuid, hub.auth, app.appId, fileAsset.assetIndex)
    if (PreferencesMap.enforce_use_external_downloader || externalDownload) {
        downloadInfoList.forEach {
            MiscellaneousUtils.accessByBrowser(it.url, context)
        }
    } else {
        val fileTasker = FileTasker(
            FileTaskerId(fileAsset.name, fileAsset),
            downloadInfoList.map { it.getDownloadInfoItem(fileAsset.name) },
            DOWNLOAD_EXTRA_CACHE_DIR
        )
        val notification = DownloadNotification(fileTasker).apply {
            waitDownloadTaskNotification()
        }
        try {
            fileTasker.startDownload(taskStartedFun, {
                taskStartFailedFun(it)
                notification.cancelNotification()
            }, downloadOb, notification.getDownloadOb())
        } catch (e: DownloadFetchError) {
            taskStartFailedFun(e)
        }
    }
}

suspend fun installFileTasker(fileTasker: FileTasker, context: Context) {
    val notification = DownloadNotificationManager.getNotification(fileTasker)
    notification?.showInstallNotification(fileTasker.name)
    fileTasker.install(context, {
        notification?.cancelNotification()
        ToastUtil.showText(context, "${context.getString(R.string.install_failed)}: ${it.msg()}")
    }, {
        notification?.cancelNotification()
        ToastUtil.showText(context, R.string.install_success)
    })
}

fun deleteFileTasker(fileTasker: FileTasker) {
    DownloadNotificationManager.getNotification(fileTasker)?.cancelNotification()
    fileTasker.cancel()
}


fun DownloadItem.getDownloadInfoItem(defName: String): DownloadInfoItem {
    return DownloadInfoItem(name ?: defName, url, headers ?: emptyMap(), cookies ?: emptyMap())
}

fun FileTasker.installable() = file.installable()

suspend fun FileTasker.install(
    context: Context,
    failedInstallObserverFun: ObserverFun<Throwable>,
    completeInstallObserverFun: ObserverFunNoArg
) {
    Installer.install(file, context, failedInstallObserverFun, completeInstallObserverFun)
}