package net.xzos.upgradeall.core.downloader.filedownloader

import net.xzos.upgradeall.core.downloader.filedownloader.item.DownloadId
import net.xzos.upgradeall.core.downloader.filedownloader.item.Downloader
import net.xzos.upgradeall.core.downloader.filedownloader.observe.DownloadRegister
import net.xzos.upgradeall.core.utils.coroutines.coroutinesMutableListOf
import java.io.IOException

internal object DownloaderManager {
    private val downloaderList = coroutinesMutableListOf<Downloader>(true)

    fun getDownloaderList(): List<Downloader> = downloaderList

    private fun getDownloader(downloadId: DownloadId): Downloader? {
        val list = downloaderList.filter { it.downloadId == downloadId }
        return if (list.isNotEmpty())
            list[0]
        else null
    }

    internal fun addDownloader(downloader: Downloader) {
        getDownloader(downloader.downloadId)?.run {
            throw MultipleSameIdDownloaderException(downloadId)
        }
        DownloadRegister.registerOb(downloader.downloadId, downloader.downloadOb)
        downloaderList.add(downloader)
    }

    internal fun removeDownloader(downloader: Downloader) {
        DownloadRegister.unRegisterById(downloader.downloadId)
        downloaderList.remove(downloader)
        renewDownloadServiceStatus()
    }
}

internal class MultipleSameIdDownloaderException(private val downloadId: DownloadId) : IOException() {
    override fun toString(): String {
        return "DownloaderList exist same id downloader($downloadId) in DownloaderManager."
    }
}