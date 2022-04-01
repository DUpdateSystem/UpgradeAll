package net.xzos.upgradeall.core.downloader.filedownloader.item

import net.xzos.upgradeall.core.utils.coroutines.coroutinesMutableListOf
import zlc.season.rxdownload4.manager.*

internal class DownloadTaskWrapper(val manager: TaskManager) {
    var snap: DownloadStatusSnap? = null

    private val subscribeList = coroutinesMutableListOf<(DownloadStatusSnap) -> Unit>()

    init {
        manager.subscribe { status ->
            val downloadStatus = when (status) {
                is Normal -> DownloadStatus.START
                is Pending -> DownloadStatus.START
                is Started -> DownloadStatus.START
                is Downloading -> DownloadStatus.RUNNING
                is Paused -> DownloadStatus.STOP
                is Completed -> DownloadStatus.COMPLETE
                is Failed -> DownloadStatus.FAIL
                is Deleted -> DownloadStatus.CANCEL
            }
            val progress = status.progress
            manager.currentStatus()
            val oldSnap = this.snap
            val snap = DownloadStatusSnap(downloadStatus, progress.downloadSize, progress.totalSize)
            oldSnap?.also { snap.countSpeed(it) }
            this.snap = snap
            subscribeList.map { it(snap) }
        }
    }


    fun subscribe(function: (DownloadStatusSnap) -> Unit) {
        subscribeList.add(function)
    }
}