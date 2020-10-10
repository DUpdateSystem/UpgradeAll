package net.xzos.upgradeall.server.downloader

import com.tonyodev.fetch2.*
import com.tonyodev.fetch2core.DownloadBlock
import net.xzos.upgradeall.core.log.Log
import net.xzos.upgradeall.core.oberver.Informer
import net.xzos.upgradeall.utils.MiscellaneousUtils


object DownloadRegister : Informer, FetchListener, FetchGroupListener {

    private const val TASK_START = "TASK_START"
    private const val TASK_RUNNING = "TASK_RUNNING"
    private const val TASK_STOP = "TASK_STOP"
    private const val TASK_COMPLETE = "TASK_COMPLETE"
    private const val TASK_CANCEL = "TASK_CANCEL"
    private const val TASK_FAIL = "TASK_FAIL"

    internal fun Int.getStartNotifyKey(): String = "$this$TASK_START"
    internal fun Int.getRunningNotifyKey(): String = "$this$TASK_RUNNING"
    internal fun Int.getStopNotifyKey(): String = "$this$TASK_STOP"
    internal fun Int.getCompleteNotifyKey(): String = "$this$TASK_COMPLETE"
    internal fun Int.getCancelNotifyKey(): String = "$this$TASK_CANCEL"
    internal fun Int.getFailNotifyKey(): String = "$this$TASK_FAIL"

    override fun onCompleted(download: Download) {
        taskComplete(download.id, download)
    }

    override fun onCompleted(groupId: Int, download: Download, fetchGroup: FetchGroup) {
        taskComplete(groupId, download)
    }

    override fun onProgress(download: Download, etaInMilliSeconds: Long, downloadedBytesPerSecond: Long) {
        taskRunning(download.id, download)
    }

    override fun onProgress(groupId: Int, download: Download, etaInMilliSeconds: Long, downloadedBytesPerSecond: Long, fetchGroup: FetchGroup) {
        taskRunning(groupId, download)
    }

    override fun onPaused(download: Download) {
        taskStop(download.id, download)
    }

    override fun onPaused(groupId: Int, download: Download, fetchGroup: FetchGroup) {
        taskStop(groupId, download)
    }

    override fun onResumed(download: Download) {
        taskRunning(download.id, download)
    }

    override fun onResumed(groupId: Int, download: Download, fetchGroup: FetchGroup) {
        taskRunning(groupId, download)
    }

    override fun onStarted(groupId: Int, download: Download, downloadBlocks: List<DownloadBlock>, totalBlocks: Int, fetchGroup: FetchGroup) {
        taskStart(groupId, download)
    }

    override fun onStarted(download: Download, downloadBlocks: List<DownloadBlock>, totalBlocks: Int) {
        taskStart(download.id, download)
    }

    override fun onCancelled(download: Download) {
        taskCancel(download.id, download)
    }

    override fun onCancelled(groupId: Int, download: Download, fetchGroup: FetchGroup) {
        taskCancel(groupId, download)
    }

    override fun onRemoved(download: Download) {
        taskCancel(download.id, download)
    }

    override fun onRemoved(groupId: Int, download: Download, fetchGroup: FetchGroup) {
        taskCancel(groupId, download)
    }

    override fun onDeleted(download: Download) {
        taskCancel(download.id, download)
    }

    override fun onDeleted(groupId: Int, download: Download, fetchGroup: FetchGroup) {
        taskCancel(groupId, download)
    }

    override fun onError(groupId: Int, download: Download, error: Error, throwable: Throwable?, fetchGroup: FetchGroup) {
        Log.e(Downloader.logTagObject, Downloader.TAG, error.toString())
        MiscellaneousUtils.showToast(error.toString())
        taskFail(groupId, download)
    }

    override fun onError(download: Download, error: Error, throwable: Throwable?) {
        Log.e(Downloader.logTagObject, Downloader.TAG, error.toString())
        MiscellaneousUtils.showToast(error.toString())
        taskFail(download.id, download)
    }

    private fun taskStart(id: Int, download: Download) {
        notifyChanged(id.getStartNotifyKey(), download)
    }

    private fun taskRunning(id: Int, download: Download) {
        notifyChanged(id.getRunningNotifyKey(), download)
    }

    private fun taskStop(id: Int, download: Download) {
        notifyChanged(id.getStopNotifyKey(), download)
    }

    private fun taskComplete(id: Int, download: Download) {
        notifyChanged(id.getCompleteNotifyKey(), download)
    }

    private fun taskCancel(id: Int, download: Download) {
        notifyChanged(id.getCancelNotifyKey(), download)
    }

    private fun taskFail(id: Int, download: Download) {
        notifyChanged(id.getFailNotifyKey(), download)
    }

    override fun onQueued(download: Download, waitingOnNetwork: Boolean) {}
    override fun onQueued(groupId: Int, download: Download, waitingNetwork: Boolean, fetchGroup: FetchGroup) {}
    override fun onDownloadBlockUpdated(groupId: Int, download: Download, downloadBlock: DownloadBlock, totalBlocks: Int, fetchGroup: FetchGroup) {}
    override fun onDownloadBlockUpdated(download: Download, downloadBlock: DownloadBlock, totalBlocks: Int) {}
    override fun onWaitingNetwork(groupId: Int, download: Download, fetchGroup: FetchGroup) {}
    override fun onWaitingNetwork(download: Download) {}
    override fun onAdded(groupId: Int, download: Download, fetchGroup: FetchGroup) {}
    override fun onAdded(download: Download) {}
}
