package net.xzos.upgradeall.ui.filemanagement

import com.tonyodev.fetch2.Status
import kotlinx.coroutines.runBlocking
import net.xzos.upgradeall.core.filetasker.FileTasker
import net.xzos.upgradeall.ui.base.list.ListItemTextView

class FileItemView(
        override val name: String,
        private val fileTasker: FileTasker,
) : ListItemTextView {
    val downloader get() = fileTasker.downloader
    val downloadingNum get() = getDownloadNumByStatus(Status.DOWNLOADING)
    val completedNum get() = getDownloadNumByStatus(Status.COMPLETED)
    val failedNum get() = getDownloadNumByStatus(Status.FAILED)
    val downloadProgress get() = runBlocking { downloader?.getDownloadProgress() }

    private fun doGetDownloadNumByStatus(status: Status): Int {
        val downloader = this.downloader ?: return 0
        return runBlocking { downloader.getDownloadList() }.filter { status == it.status }.size
    }

    private fun getDownloadNumByStatus(status: Status): String {
        return doGetDownloadNumByStatus(status).toString()
    }
}