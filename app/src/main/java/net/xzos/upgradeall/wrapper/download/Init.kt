package net.xzos.upgradeall.wrapper.download

import android.content.Context
import net.xzos.upgradeall.core.module.app.App
import net.xzos.upgradeall.core.module.app.version_item.FileAsset
import net.xzos.upgradeall.server.downloader.DownloadNotification

suspend fun startDownload(
    context: Context, externalDownload: Boolean,
    app: App, fileAsset: FileAsset
): DownloadTasker {
    val wrapper = DownloadTasker(app, fileAsset)
    DownloadNotification(wrapper).apply {
        registerNotify(wrapper)
    }
    wrapper.start(context, externalDownload)
    return wrapper
}
