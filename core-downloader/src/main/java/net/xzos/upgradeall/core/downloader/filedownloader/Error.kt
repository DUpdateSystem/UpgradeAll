package net.xzos.upgradeall.core.downloader.filedownloader

import androidx.documentfile.provider.DocumentFile
import net.xzos.upgradeall.core.downloader.filedownloader.item.Downloader
import java.io.IOException

private const val DOWNLOAD_CANCELLED = "DOWNLOAD_CANCELLED"

internal class MultipleSameIdDownloaderException(private val downloader: Downloader) :
    IOException() {
    override fun toString(): String {
        return "DownloaderList exist same id downloader(${downloader.id}) in DownloaderManager."
    }
}

class DownloadFileError internal constructor(
    val parent: DocumentFile, val fileName: String
) : RuntimeException()

class DownloadCanceledError internal constructor(
    val msg: String? = null
) : RuntimeException(DOWNLOAD_CANCELLED)

class DownloadFetchError internal constructor(
    val error: Error
) : RuntimeException(DOWNLOAD_CANCELLED)