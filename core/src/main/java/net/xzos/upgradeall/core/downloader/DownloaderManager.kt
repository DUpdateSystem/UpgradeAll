package net.xzos.upgradeall.core.downloader

import net.xzos.upgradeall.core.utils.coroutines.coroutinesMutableListOf
import java.io.IOException

internal object DownloaderManager {
    private val downloaderList = coroutinesMutableListOf<Downloader>(true)

    fun getDownloaderList(): List<Downloader> = downloaderList

    fun getDownloader(downloadId: DownloadId): Downloader? {
        val list = downloaderList.filter { it.downloadId == downloadId }
        return if (list.isNotEmpty())
            list[0]
        else null
    }

    internal fun setDownloader(downloader: Downloader) {
        getDownloader(downloader.downloadId)?.run {
            throw MultipleSameIdDownloaderException(downloadId)
        }
        downloaderList.add(downloader)
    }

    internal fun removeDownloader(downloader: Downloader) {
        downloaderList.remove(downloader)
        if (downloaderList.isEmpty())
            DownloadService.close()
    }
}

internal class MultipleSameIdDownloaderException(private val downloadId: DownloadId) : IOException() {
    override fun toString(): String {
        return "DownloaderList exist same id downloader($downloadId) in DownloaderManager."
    }
}