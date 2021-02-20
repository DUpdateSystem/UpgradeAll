package net.xzos.upgradeall.server.downloader

import net.xzos.upgradeall.core.downloader.DownloadOb
import net.xzos.upgradeall.core.filetasker.FileTasker.Companion.getFileTasker
import net.xzos.upgradeall.core.module.app.FileAsset

suspend fun download(
        fileAsset: FileAsset,
        taskStartedFun: (Int) -> Unit,
        taskStartFailedFun: () -> Unit,
        downloadOb: DownloadOb,
) {
    val fileTasker = fileAsset.getFileTasker()
    val notification = DownloadNotification(fileTasker).apply {
        waitDownloadTaskNotification(fileAsset.name)
    }
    fileTasker.startDownload(taskStartedFun, {
        taskStartFailedFun()
        notification.cancelNotification()
    }, downloadOb, notification.getDownloadOb())
}