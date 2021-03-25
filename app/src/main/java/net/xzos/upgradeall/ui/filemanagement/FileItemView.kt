package net.xzos.upgradeall.ui.filemanagement

import com.tonyodev.fetch2.Status
import net.xzos.upgradeall.core.downloader.Downloader
import net.xzos.upgradeall.core.filetasker.FileTasker
import net.xzos.upgradeall.ui.base.list.ListItemTextView

class FileItemView(
        override val appName: String,
        val fileTasker: FileTasker,
) : ListItemTextView {
    private val numUtil = DownloadTaskerNumUtil(fileTasker.downloader)
    suspend fun getDownloadingNum() = numUtil.getDownloadingNum().toString()
    suspend fun getPauseNum() = numUtil.getPauseNum().toString()
    suspend fun getCompletedNum() = numUtil.getCompletedNum().toString()
    suspend fun getFailedNum() = numUtil.getFailedNum().toString()
    suspend fun getDownloadProgress() = numUtil.getDownloadProgress()
}

class DownloadTaskerNumUtil(private val downloader: Downloader?) {

    suspend fun getDownloadingNum() = getDownloadNumByStatus(Status.DOWNLOADING)
    suspend fun getPauseNum() = getDownloadNumByStatus(Status.PAUSED)
    suspend fun getCompletedNum() = getDownloadNumByStatus(Status.COMPLETED)
    suspend fun getFailedNum() = getDownloadNumByStatus(Status.FAILED)
    suspend fun getDownloadProgress() = downloader?.getDownloadProgress() ?: -1

    private suspend fun getDownloadNumByStatus(status: Status): Int {
        return this.downloader?.getDownloadNumByStatus(status) ?: 0
    }

    private suspend fun Downloader.getDownloadNumByStatus(status: Status): Int {
        return getDownloadList().filter { status == it.status }.size
    }
}