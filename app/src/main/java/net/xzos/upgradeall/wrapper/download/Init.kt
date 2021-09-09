package net.xzos.upgradeall.wrapper.download

import android.content.Context
import net.xzos.upgradeall.core.downloader.filetasker.FileTasker
import net.xzos.upgradeall.core.module.app.App
import net.xzos.upgradeall.core.module.app.version_item.FileAsset
import net.xzos.upgradeall.server.downloader.DownloadNotification
import net.xzos.upgradeall.wrapper.download.status.DownloadInformer

suspend fun startDownload(
    context: Context, externalDownload: Boolean,
    app: App, fileAsset: FileAsset
): FileTasker? {
    val downloadInformer = DownloadInformer()
    DownloadNotification().apply {
        observeDownloadTasker(downloadInformer)
    }
    return downloadInformer.start(context, externalDownload, app, fileAsset)
}
