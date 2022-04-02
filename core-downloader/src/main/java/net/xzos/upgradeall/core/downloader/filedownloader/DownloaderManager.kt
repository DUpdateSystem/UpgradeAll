package net.xzos.upgradeall.core.downloader.filedownloader

import net.xzos.upgradeall.core.downloader.filedownloader.item.Downloader
import net.xzos.upgradeall.core.downloader.filedownloader.observe.DownloadRegister
import net.xzos.upgradeall.core.utils.coroutines.coroutinesMutableListOf
import java.io.IOException

internal object DownloaderManager {
    private val downloaderList = coroutinesMutableListOf<Downloader>(true)

    fun getDownloaderList(): List<Downloader> = downloaderList

    private fun getDownloader(downloadId: Int): Downloader? {
        val list = downloaderList.filter { it.id == downloadId }
        return if (list.isNotEmpty())
            list[0]
        else null
    }

    internal fun addDownloader(downloader: Downloader) {
        if (!downloaderList.add(downloader))
            throw MultipleSameIdDownloaderException(downloader)
        DownloadRegister.registerDownloader(downloader)
    }

    internal fun removeDownloader(downloader: Downloader) {
        DownloadRegister.unRegisterId(downloader)
        downloaderList.remove(downloader)
    }
}

internal class MultipleSameIdDownloaderException(private val downloader: Downloader) :
    IOException() {
    override fun toString(): String {
        return "DownloaderList exist same id downloader(${downloader.id}) in DownloaderManager."
    }
}