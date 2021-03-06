package net.xzos.upgradeall.server.downloader

import net.xzos.upgradeall.R
import net.xzos.upgradeall.application.MyApplication
import net.xzos.upgradeall.core.downloader.DownloadOb
import net.xzos.upgradeall.core.filetasker.FileTasker
import net.xzos.upgradeall.core.filetasker.FileTasker.Companion.getFileTasker
import net.xzos.upgradeall.core.log.msg
import net.xzos.upgradeall.core.module.app.FileAsset
import net.xzos.upgradeall.utils.MiscellaneousUtils

suspend fun startDownload(
        fileAsset: FileAsset,
        taskStartedFun: (Int) -> Unit,
        taskStartFailedFun: () -> Unit,
        downloadOb: DownloadOb,
) {
    val fileTasker = fileAsset.getFileTasker()
    val notification = DownloadNotification(fileTasker).apply {
        waitDownloadTaskNotification()
    }
    fileTasker.startDownload(taskStartedFun, {
        taskStartFailedFun()
        notification.cancelNotification()
    }, downloadOb, notification.getDownloadOb())
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