package net.xzos.upgradeall.ui.filemanagement

import com.tonyodev.fetch2.Status
import kotlinx.coroutines.runBlocking
import net.xzos.upgradeall.core.downloader.Downloader
import net.xzos.upgradeall.ui.base.list.ListItemTextView

class FileItemView(
        override val name: String,
        val downloader: Downloader,
) : ListItemTextView {
    val downloadingNum get() = runBlocking { downloader.getDownloadList() }.filter { it.status == Status.DOWNLOADING }.size
    val completedNum get() = runBlocking { downloader.getDownloadList() }.filter { it.status == Status.COMPLETED }.size
    val failedNum get() = runBlocking { downloader.getDownloadList() }.filter { it.status == Status.FAILED }.size
    val downloadProgress get() = runBlocking { downloader.getDownloadProgress() }
}