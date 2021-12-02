package net.xzos.upgradeall.core.downloader.filedownloader.observe

import com.tonyodev.fetch2.*
import com.tonyodev.fetch2core.DownloadBlock
import net.xzos.upgradeall.core.downloader.filedownloader.item.DownloadId
import net.xzos.upgradeall.core.downloader.filedownloader.item.Downloader
import net.xzos.upgradeall.core.utils.coroutines.coroutinesMutableMapOf
import net.xzos.upgradeall.core.utils.log.Log
import net.xzos.upgradeall.core.utils.log.msg
import net.xzos.upgradeall.core.utils.oberver.Informer
import net.xzos.upgradeall.core.utils.oberver.Tag

private enum class DownloadStatus : Tag {
    TASK_START,
    TASK_RUNNING,
    TASK_STOP,
    TASK_COMPLETE,
    TASK_CANCEL,
    TASK_FAIL,
}

internal object DownloadRegister : FetchListener, FetchGroupListener {

    private val informerMap = coroutinesMutableMapOf<DownloadId, Informer>(true)

    private fun newDownloadInformer(): Informer {
        return object : Informer {
            override val informerId = Informer.getInformerId()
        }
    }

    fun registerOb(downloadId: DownloadId, downloadOb: DownloadOb) {
        with(informerMap.getOrDefault(downloadId) { newDownloadInformer() }) {
            observeForever(DownloadStatus.TASK_START, downloadOb.startFunc)
            observeForever(DownloadStatus.TASK_RUNNING, downloadOb.runningFunc)
            observeForever(DownloadStatus.TASK_STOP, downloadOb.stopFunc)
            observeForever(DownloadStatus.TASK_COMPLETE, downloadOb.completeFunc)
            observeForever(DownloadStatus.TASK_CANCEL, downloadOb.cancelFunc)
            observeForever(DownloadStatus.TASK_FAIL, downloadOb.failFunc)
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

    override fun onCompleted(download: Download) {
        taskComplete(DownloadId(false, download.id), download)
    }

    override fun onCompleted(groupId: Int, download: Download, fetchGroup: FetchGroup) {
        taskComplete(DownloadId(false, groupId), download)
    }

    override fun onProgress(
        download: Download,
        etaInMilliSeconds: Long,
        downloadedBytesPerSecond: Long
    ) {
        taskRunning(DownloadId(false, download.id), download)
    }

    override fun onProgress(
        groupId: Int,
        download: Download,
        etaInMilliSeconds: Long,
        downloadedBytesPerSecond: Long,
        fetchGroup: FetchGroup
    ) {
        taskRunning(DownloadId(false, groupId), download)
    }

    override fun onPaused(download: Download) {
        taskStop(DownloadId(false, download.id), download)
    }

    override fun onPaused(groupId: Int, download: Download, fetchGroup: FetchGroup) {
        taskStop(DownloadId(false, groupId), download)
    }

    override fun onResumed(download: Download) {
        taskRunning(DownloadId(false, download.id), download)
    }

    override fun onResumed(groupId: Int, download: Download, fetchGroup: FetchGroup) {
        taskRunning(DownloadId(true, groupId), download)
    }

    override fun onStarted(
        download: Download,
        downloadBlocks: List<DownloadBlock>,
        totalBlocks: Int
    ) {
        taskStart(DownloadId(false, download.id), download)
    }

    override fun onStarted(
        groupId: Int,
        download: Download,
        downloadBlocks: List<DownloadBlock>,
        totalBlocks: Int,
        fetchGroup: FetchGroup
    ) {
        taskStart(DownloadId(true, groupId), download)
    }

    override fun onCancelled(download: Download) {
        taskCancel(DownloadId(false, download.id), download)
    }

    override fun onCancelled(groupId: Int, download: Download, fetchGroup: FetchGroup) {
        taskCancel(DownloadId(true, groupId), download)
    }

    override fun onRemoved(download: Download) {
        taskCancel(DownloadId(false, download.id), download)
    }

    override fun onRemoved(groupId: Int, download: Download, fetchGroup: FetchGroup) {
        taskCancel(DownloadId(true, groupId), download)
    }

    override fun onDeleted(download: Download) {
        taskCancel(DownloadId(false, download.id), download)
    }

    override fun onDeleted(groupId: Int, download: Download, fetchGroup: FetchGroup) {
        taskCancel(DownloadId(true, groupId), download)
    }

    override fun onError(download: Download, error: Error, throwable: Throwable?) {
        Log.e(
            Downloader.logTagObject,
            Downloader.TAG,
            "error: $error, throwable: ${throwable?.msg()}"
        )
        taskFail(DownloadId(false, download.id), download)
    }

    override fun onError(
        groupId: Int,
        download: Download,
        error: Error,
        throwable: Throwable?,
        fetchGroup: FetchGroup
    ) {
        Log.e(
            Downloader.logTagObject,
            Downloader.TAG,
            "error: $error, throwable: ${throwable?.msg()}"
        )
        taskFail(DownloadId(true, groupId), download)
    }

    private fun taskStart(id: DownloadId, download: Download) {
        informerMap[id]?.run {
            notifyChanged(DownloadStatus.TASK_START, download)
        }
    }

    private fun taskRunning(id: DownloadId, download: Download) {
        informerMap[id]?.run {
            notifyChanged(DownloadStatus.TASK_RUNNING, download)
        }
    }

    private fun taskStop(id: DownloadId, download: Download) {
        informerMap[id]?.run {
            notifyChanged(DownloadStatus.TASK_STOP, download)
        }
    }

    private fun taskComplete(id: DownloadId, download: Download) {
        informerMap[id]?.run {
            notifyChanged(DownloadStatus.TASK_COMPLETE, download)
        }
    }

    private fun taskCancel(id: DownloadId, download: Download) {
        informerMap[id]?.run {
            notifyChanged(DownloadStatus.TASK_CANCEL, download)
        }
    }

    private fun taskFail(id: DownloadId, download: Download) {
        informerMap[id]?.run {
            notifyChanged(DownloadStatus.TASK_FAIL, download)
        }
    }

    override fun onQueued(download: Download, waitingOnNetwork: Boolean) {}
    override fun onQueued(
        groupId: Int,
        download: Download,
        waitingNetwork: Boolean,
        fetchGroup: FetchGroup
    ) {
    }

    override fun onDownloadBlockUpdated(
        groupId: Int,
        download: Download,
        downloadBlock: DownloadBlock,
        totalBlocks: Int,
        fetchGroup: FetchGroup
    ) {
    }

    override fun onDownloadBlockUpdated(
        download: Download,
        downloadBlock: DownloadBlock,
        totalBlocks: Int
    ) {
    }

    override fun onWaitingNetwork(groupId: Int, download: Download, fetchGroup: FetchGroup) {}
    override fun onWaitingNetwork(download: Download) {}
    override fun onAdded(groupId: Int, download: Download, fetchGroup: FetchGroup) {}
    override fun onAdded(download: Download) {}
}