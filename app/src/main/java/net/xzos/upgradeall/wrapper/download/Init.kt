package net.xzos.upgradeall.wrapper.download

import android.content.Context
import net.xzos.upgradeall.core.downloader.filetasker.FileTasker
import net.xzos.upgradeall.core.module.app.App
import net.xzos.upgradeall.core.module.app.version_item.FileAsset
import net.xzos.upgradeall.server.downloader.DownloadNotification

suspend fun startDownload(
    context: Context, externalDownload: Boolean,
    app: App, fileAsset: FileAsset
): FileTasker {
    val wrapper = FileTaskerWrapper(app, fileAsset)
    DownloadNotification(wrapper.id).apply {
        registerNotify(wrapper)
    }
    wrapper.start(context, externalDownload)
    return wrapper
}
