package net.xzos.upgradeall.server.downloader

import net.xzos.upgradeall.core.downloader.DownloadOb
import net.xzos.upgradeall.core.module.app.FileAsset

suspend fun download(
        fileAsset: FileAsset,
        taskStartedFun: (Int) -> Unit,
        taskStartFailedFun: () -> Unit,
        downloadOb: DownloadOb,
) {
    val notification = DownloadNotification(fileAsset).apply {
        waitDownloadTaskNotification(fileAsset.name)
    }
    fileAsset.download(taskStartedFun, {
        taskStartFailedFun()
        notification.cancelNotification()
    }, downloadOb, notification.getDownloadOb())
}