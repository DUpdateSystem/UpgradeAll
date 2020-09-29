package net.xzos.upgradeall.server.downloader

import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.FetchListener
import com.tonyodev.fetch2core.DownloadBlock
import net.xzos.upgradeall.core.log.Log
import net.xzos.upgradeall.core.oberver.Informer
import net.xzos.upgradeall.utils.MiscellaneousUtils


object DownloadRegister : Informer, FetchListener {

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
    override fun onQueued(download: Download, waitingOnNetwork: Boolean) {}

    override fun onCompleted(download: Download) {
        taskComplete(download)
    }

    override fun onProgress(download: Download, etaInMilliSeconds: Long, downloadedBytesPerSecond: Long) {
        taskRunning(download)
    }

    override fun onPaused(download: Download) {
        taskStop(download)
    }

    override fun onResumed(download: Download) {
        taskRunning(download)
    }

    override fun onStarted(download: Download, downloadBlocks: List<DownloadBlock>, totalBlocks: Int) {
        taskStart(download)
    }

    override fun onWaitingNetwork(download: Download) {}

    override fun onAdded(download: Download) {}

    override fun onCancelled(download: Download) {
        taskCancel(download)
    }

    override fun onRemoved(download: Download) {
        taskCancel(download)
    }

    override fun onDeleted(download: Download) {
        taskCancel(download)
    }

    override fun onDownloadBlockUpdated(download: Download, downloadBlock: DownloadBlock, totalBlocks: Int) {
    }

    override fun onError(download: Download, error: com.tonyodev.fetch2.Error, throwable: Throwable?) {
        Log.e(Downloader.logTagObject, Downloader.TAG, error.toString())
        MiscellaneousUtils.showToast(error.toString())
        taskFail(download)
    }

    private fun taskStart(task: Download) {
        notifyChanged(task.id.getStartNotifyKey(), task)
    }

    private fun taskRunning(task: Download) {
        notifyChanged(task.id.getRunningNotifyKey(), task)
    }

    private fun taskStop(task: Download) {
        notifyChanged(task.id.getStopNotifyKey(), task)
    }

    private fun taskComplete(task: Download) {
        notifyChanged(task.id.getCompleteNotifyKey(), task)
    }

    private fun taskCancel(task: Download) {
        notifyChanged(task.id.getCancelNotifyKey(), task)
    }

    private fun taskFail(task: Download?) {
        task ?: return
        notifyChanged(task.id.getFailNotifyKey(), task)
    }
}
