package net.xzos.upgradeall.wrapper.download.status

import net.xzos.upgradeall.core.downloader.filedownloader.observe.DownloadOb
import net.xzos.upgradeall.core.downloader.filetasker.FileTasker
import net.xzos.upgradeall.core.downloader.filetasker.FileTaskerId
import net.xzos.upgradeall.core.utils.oberver.Informer


class DownloadInformer(val id: FileTaskerId) : Informer {

    override val informerId = Informer.getInformerId()

    private lateinit var fileTasker: FileTasker

    val downloadOb by lazy {
        DownloadOb(
            startFunc = { notifyChanged(DownloadStatus.DOWNLOAD_START, it) },
            runningFunc = { notifyChanged(DownloadStatus.DOWNLOADING, it) },
            stopFunc = { notifyChanged(DownloadStatus.DOWNLOAD_STOP, it) },
            completeFunc = {
                notifyChanged(DownloadStatus.DOWNLOAD_COMPLETE, it)
                unregister()
            },
            cancelFunc = {
                notifyChanged(DownloadStatus.DOWNLOAD_CANCEL, it)
                unregister()
            },
            failFunc = { notifyChanged(DownloadStatus.DOWNLOAD_FAIL, it) },
        )
    }

    fun register(fileTasker: FileTasker) {
        fileTasker.downloader.register(downloadOb)
        DownloadInformerManager.add(fileTasker.id.toString(), this)
        this.fileTasker = fileTasker
    }

    fun unregister() {
        fileTasker.downloader.unregister(downloadOb)
        DownloadInformerManager.remove(this)
    }
}