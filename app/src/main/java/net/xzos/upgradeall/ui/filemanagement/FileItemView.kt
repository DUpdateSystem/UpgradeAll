package net.xzos.upgradeall.ui.filemanagement

import com.tonyodev.fetch2.Status
import net.xzos.upgradeall.core.filetasker.FileTasker
import net.xzos.upgradeall.ui.base.list.ListItemTextView

class FileItemView(
        override val name: String,
        private val fileTasker: FileTasker,
) : ListItemTextView {
    val downloader get() = fileTasker.downloader
    suspend fun getDownloadingNum() = getDownloadNumByStatus(Status.DOWNLOADING)
    suspend fun getCompletedNum() = getDownloadNumByStatus(Status.COMPLETED)
    suspend fun getFailedNum() = getDownloadNumByStatus(Status.FAILED)
    suspend fun getDownloadProgress() = downloader?.getDownloadProgress() ?: -1

    private suspend fun getDownloadNumByStatus(status: Status): String {
        val downloader = this.downloader ?: return "0"
        val num = downloader.getDownloadList().filter { status == it.status }.size
        return num.toString()
    }
}