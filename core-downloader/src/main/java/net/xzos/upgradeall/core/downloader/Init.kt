package net.xzos.upgradeall.core.downloader

import android.annotation.SuppressLint
import android.app.Notification
import net.xzos.upgradeall.core.downloader.service.DownloadService

@SuppressLint("StaticFieldLeak")
lateinit var downloadConfig: DownloadConfig

fun initDownload(
    _downloadConfig: DownloadConfig,
) {
    downloadConfig = _downloadConfig
}

fun setDownloadServer(serverNotificationMaker: () -> Pair<Int, Notification>) {
    DownloadService.setNotificationMaker(serverNotificationMaker)
}
