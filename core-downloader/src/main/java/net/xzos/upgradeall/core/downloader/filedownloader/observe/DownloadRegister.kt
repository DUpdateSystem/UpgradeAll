package net.xzos.upgradeall.core.downloader.filedownloader.observe

import net.xzos.upgradeall.core.downloader.filedownloader.item.DownloadId
import net.xzos.upgradeall.core.downloader.filedownloader.item.DownloadStatus
import net.xzos.upgradeall.core.downloader.filedownloader.item.DownloadStatusSnap
import net.xzos.upgradeall.core.downloader.filedownloader.item.Downloader
import net.xzos.upgradeall.core.utils.coroutines.coroutinesMutableMapOf
import net.xzos.upgradeall.core.utils.oberver.Informer

internal object DownloadRegister {

    private val informerMap = coroutinesMutableMapOf<DownloadId, Informer>(true)

    private fun newDownloadInformer(): Informer {
        return object : Informer {
            override val informerId = Informer.getInformerId()
        }
    }

    internal fun registerDownloader(downloader: Downloader) {
        val id = downloader.downloadId
        downloader.taskList.forEach { taskWrapper ->
            taskWrapper.subscribe { snap ->
                val statusList = downloader.taskList.mapNotNull { it.snap }
                when (snap.status) {
                    DownloadStatus.START -> {
                        taskStart(id, statusList)
                    }
                    DownloadStatus.RUNNING -> {
                        taskRunning(id, statusList)
                    }
                    DownloadStatus.STOP -> {
                        taskStop(id, statusList)
                    }
                    DownloadStatus.COMPLETE -> {
                        taskComplete(id, statusList)
                    }
                    DownloadStatus.FAIL -> {
                        taskFail(id, statusList)
                    }
                    DownloadStatus.CANCEL -> {
                        taskCancel(id, statusList)
                    }
                }
            }
        }
    }

    fun registerOb(downloadId: DownloadId, downloadOb: DownloadOb) {
        with(informerMap.getOrDefault(downloadId) { newDownloadInformer() }) {
            observeForever(DownloadStatus.START, downloadOb.startFunc)
            observeForever(DownloadStatus.RUNNING, downloadOb.runningFunc)
            observeForever(DownloadStatus.STOP, downloadOb.stopFunc)
            observeForever(DownloadStatus.COMPLETE, downloadOb.completeFunc)
            observeForever(DownloadStatus.CANCEL, downloadOb.cancelFunc)
            observeForever(DownloadStatus.FAIL, downloadOb.failFunc)
        }
    }

    fun unRegisterId(downloadId: DownloadId) {
        informerMap.remove(downloadId)
    }

    fun unRegisterOb(downloadId: DownloadId, downloadOb: DownloadOb) {
        informerMap[downloadId]?.run {
            removeObserver(downloadOb.startFunc)
            removeObserver(downloadOb.runningFunc)
            removeObserver(downloadOb.stopFunc)
            removeObserver(downloadOb.completeFunc)
            removeObserver(downloadOb.cancelFunc)
            removeObserver(downloadOb.failFunc)
        }
    }

    private fun taskStart(id: DownloadId, snap: List<DownloadStatusSnap>) {
        informerMap[id]?.run {
            notifyChanged(DownloadStatus.START, snap)
        }
    }

    private fun taskRunning(id: DownloadId, status: List<DownloadStatusSnap>) {
        informerMap[id]?.run {
            notifyChanged(DownloadStatus.RUNNING, status)
        }
    }

    private fun taskStop(id: DownloadId, status: List<DownloadStatusSnap>) {
        informerMap[id]?.run {
            notifyChanged(DownloadStatus.STOP, status)
        }
    }

    private fun taskComplete(id: DownloadId, status: List<DownloadStatusSnap>) {
        informerMap[id]?.run {
            notifyChanged(DownloadStatus.COMPLETE, status)
        }
    }

    private fun taskCancel(id: DownloadId, status: List<DownloadStatusSnap>) {
        informerMap[id]?.run {
            notifyChanged(DownloadStatus.CANCEL, status)
        }
    }

    private fun taskFail(id: DownloadId, status: List<DownloadStatusSnap>) {
        informerMap[id]?.run {
            notifyChanged(DownloadStatus.FAIL, status)
        }
    }
}