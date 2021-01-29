package net.xzos.upgradeall.ui.viewmodels.view

import com.tonyodev.fetch2.Status
import kotlinx.coroutines.runBlocking
import net.xzos.upgradeall.core.downloader.Downloader

class FileItemView(
        name: String,
        val downloader: Downloader,
) : ListItemView(name) {
    val downloadingNum get() = runBlocking { downloader.getDownloadList() }.filter { it.status == Status.DOWNLOADING }.size
    val completedNum get() = runBlocking { downloader.getDownloadList() }.filter { it.status == Status.COMPLETED }.size
    val failedNum get() = runBlocking { downloader.getDownloadList() }.filter { it.status == Status.FAILED }.size
    val downloadProgress get() = runBlocking { downloader.getDownloadProgress() }
}