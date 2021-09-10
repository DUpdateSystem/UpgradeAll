package net.xzos.upgradeall.wrapper.download

import android.content.Context
import net.xzos.upgradeall.core.downloader.filetasker.FileTasker
import net.xzos.upgradeall.core.module.app.App
import net.xzos.upgradeall.core.module.app.version_item.FileAsset
import net.xzos.upgradeall.server.downloader.DownloadNotification
import net.xzos.upgradeall.wrapper.download.status.DownloadStarter

suspend fun startDownload(
    context: Context, externalDownload: Boolean,
    app: App, fileAsset: FileAsset
): FileTasker? {
    val starter = DownloadStarter(app, fileAsset)
    DownloadNotification(starter.id).apply {
        observeDownloadTasker(starter.downloadInformer)
    }
    return starter.start(context, externalDownload)
}
