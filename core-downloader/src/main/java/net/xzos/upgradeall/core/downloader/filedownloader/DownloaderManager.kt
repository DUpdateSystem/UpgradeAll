package net.xzos.upgradeall.core.downloader.filedownloader

import net.xzos.upgradeall.core.downloader.filedownloader.item.Downloader
import net.xzos.upgradeall.core.utils.coroutines.coroutinesMutableListOf
import java.io.File

object DownloaderManager {
    private val downloaderList = coroutinesMutableListOf<Downloader>(true)

    fun getDownloaderList(): List<Downloader> = downloaderList

    fun getDownloader(downloadId: Int): Downloader? {
        val list = downloaderList.filter { it.id == downloadId }
        return if (list.isNotEmpty())
            list[0]
        else null
    }

    internal fun addDownloader(downloader: Downloader) {
        if (!downloaderList.add(downloader))
            throw MultipleSameIdDownloaderException(downloader)
    }

    fun removeDownloader(downloader: Downloader) {
        downloaderList.remove(downloader)
    }
}

fun DownloaderManager.newDownloader(downloadDir: File) = Downloader(downloadDir).apply {
    addDownloader(this)
}