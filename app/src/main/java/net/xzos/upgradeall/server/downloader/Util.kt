package net.xzos.upgradeall.server.downloader

import android.content.Context
import net.xzos.upgradeall.R
import net.xzos.upgradeall.application.MyApplication
import net.xzos.upgradeall.core.downloader.DownloadFetchError
import net.xzos.upgradeall.core.downloader.DownloadOb
import net.xzos.upgradeall.core.downloader.PreDownload
import net.xzos.upgradeall.core.filetasker.FileTasker
import net.xzos.upgradeall.core.filetasker.FileTasker.Companion.getFileTasker
import net.xzos.upgradeall.core.log.msg
import net.xzos.upgradeall.core.module.app.FileAsset
import net.xzos.upgradeall.data.PreferencesMap
import net.xzos.upgradeall.utils.MiscellaneousUtils

suspend fun startDownload(
        fileAsset: FileAsset,
        taskStartedFun: (Int) -> Unit,
        taskStartFailedFun: (Throwable) -> Unit,
        downloadOb: DownloadOb,
        context: Context,
        externalDownload: Boolean,
) {
    MiscellaneousUtils.showToast(R.string.download_loading)
    val downloadInfoList = PreDownload.getDownloadInfoList(fileAsset)
    if (PreferencesMap.enforce_use_external_downloader || externalDownload) {
        downloadInfoList.forEach {
            MiscellaneousUtils.accessByBrowser(it.url, context)
        }
    } else {
        val fileTasker = fileAsset.getFileTasker(downloadInfoList)
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

suspend fun installFileTasker(fileTasker: FileTasker) {
    val notification = DownloadNotificationManager.getNotification(fileTasker)
    notification?.showInstallNotification(fileTasker.name)
    fileTasker.install({
        notification?.cancelNotification()
        MiscellaneousUtils.showToast("${MyApplication.context.getString(R.string.install_failed)}: ${it.msg()}")
    }, {
        notification?.cancelNotification()
        MiscellaneousUtils.showToast(R.string.install_success)
    })
}

fun deleteFileTasker(fileTasker: FileTasker) {
    DownloadNotificationManager.getNotification(fileTasker)?.cancelNotification()
    fileTasker.cancel()
}