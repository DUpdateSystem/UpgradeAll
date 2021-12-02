package net.xzos.upgradeall.core.downloader.filetasker

import com.tonyodev.fetch2.Download
import kotlinx.coroutines.runBlocking
import net.xzos.upgradeall.core.downloader.filedownloader.item.Downloader
import net.xzos.upgradeall.core.utils.oberver.Tag

class FileTaskerSnapBuilder(private val getDownloader: () -> Downloader?) {
    fun build(
        status: Tag,
        download: Download? = null,
        error: Throwable? = null,
        statusMsg: String = error?.stackTraceToString()?: "",
    ) = FileTaskerSnap(
        status, download, error, statusMsg
    ) { runBlocking { getDownloader()?.getDownloadList() } }

    fun buildEmpty() = build(FileTaskerStatus.NONE, statusMsg = "nothing to do")
}

class FileTaskerSnap internal constructor(
    val status: Tag,
    val download: Download? = null,
    val error: Throwable? = null,
    val statusMsg: String = "",
    private val getDownloaderList: () -> List<Download>?,
) {
    val downloadList by lazy { getDownloaderList() }
}