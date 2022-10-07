package net.xzos.upgradeall.wrapper.download

import android.content.Context
import net.xzos.upgradeall.core.module.app.App
import net.xzos.upgradeall.core.module.app.version.AssetWrapper
import net.xzos.upgradeall.server.downloader.DownloadNotification

suspend fun startDownload(
    context: Context, externalDownload: Boolean,
    app: App, wrapper: AssetWrapper
): DownloadTasker {
    val downloadTasker = DownloadTasker(app, wrapper)
    // register
    DownloadNotification(downloadTasker)
    DownloadTaskerManager.register(downloadTasker)
    //start
    downloadTasker.start(context, externalDownload)
    return downloadTasker
}
