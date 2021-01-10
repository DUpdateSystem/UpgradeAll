package net.xzos.upgradeall.core.downloader

import com.tonyodev.fetch2.*
import com.tonyodev.fetch2core.DownloadBlock
import net.xzos.upgradeall.core.log.Log
import net.xzos.upgradeall.core.utils.FuncR
import net.xzos.upgradeall.core.utils.coroutines.CoroutinesMutableMap
import net.xzos.upgradeall.core.utils.coroutines.coroutinesMutableMapOf
import net.xzos.upgradeall.core.utils.oberver.Informer
import net.xzos.upgradeall.core.utils.oberver.ObserverFun


internal object DownloadRegister : Informer, FetchListener, FetchGroupListener {

    private val downloadObFunMap: CoroutinesMutableMap<FuncR<Download>, ObserverFun<Download>> =
        coroutinesMutableMapOf(true)

    private const val TASK_START = "TASK_START"
    private const val TASK_RUNNING = "TASK_RUNNING"
    private const val TASK_STOP = "TASK_STOP"
    private const val TASK_COMPLETE = "TASK_COMPLETE"
    private const val TASK_CANCEL = "TASK_CANCEL"
    private const val TASK_FAIL = "TASK_FAIL"

    private fun DownloadId.getStartNotifyKey(): String = "$this$TASK_START"
    private fun DownloadId.getRunningNotifyKey(): String = "$this$TASK_RUNNING"
    private fun DownloadId.getStopNotifyKey(): String = "$this$TASK_STOP"
    private fun DownloadId.getCompleteNotifyKey(): String = "$this$TASK_COMPLETE"
    private fun DownloadId.getCancelNotifyKey(): String = "$this$TASK_CANCEL"
    private fun DownloadId.getFailNotifyKey(): String = "$this$TASK_FAIL"

    fun registerOb(downloadId: DownloadId, downloadOb: DownloadOb) {
        observeForever(downloadId.getStartNotifyKey(), fun(download: Download) {
            downloadOb.startFunc.call(download)
        }.also {
            downloadObFunMap[downloadOb.startFunc] = it
        })
        observeForever(downloadId.getRunningNotifyKey(), fun(download: Download) {
            downloadOb.runningFunc.call(download)
        }.also {
            downloadObFunMap[downloadOb.runningFunc] = it
        })
        observeForever(downloadId.getStopNotifyKey(), fun(download: Download) {
            downloadOb.stopFunc.call(download)
        }.also {
            downloadObFunMap[downloadOb.stopFunc] = it
        })
        observeForever(downloadId.getCompleteNotifyKey(), fun(download: Download) {
            downloadOb.completeFunc.call(download)
        }.also {
            downloadObFunMap[downloadOb.completeFunc] = it
        })
        observeForever(downloadId.getCancelNotifyKey(), fun(download: Download) {
            downloadOb.cancelFunc.call(download)
        }.also {
            downloadObFunMap[downloadOb.cancelFunc] = it
        })
        observeForever(downloadId.getFailNotifyKey(), fun(download: Download) {
            downloadOb.failFunc.call(download)
        }.also {
            downloadObFunMap[downloadOb.failFunc] = it
        })
    }

    fun unRegisterById(downloadId: DownloadId) {
        removeObserver(downloadId.getStartNotifyKey())
        removeObserver(downloadId.getRunningNotifyKey())
        removeObserver(downloadId.getStopNotifyKey())
        removeObserver(downloadId.getCompleteNotifyKey())
        removeObserver(downloadId.getCancelNotifyKey())
        removeObserver(downloadId.getFailNotifyKey())
    }

    fun unRegisterByOb(downloadOb: DownloadOb) {
        downloadObFunMap[downloadOb.startFunc]?.run {
            removeObserver(this)
        }
        downloadObFunMap[downloadOb.runningFunc]?.run {
            removeObserver(this)
        }
        downloadObFunMap[downloadOb.stopFunc]?.run {
            removeObserver(this)
        }
        downloadObFunMap[downloadOb.completeFunc]?.run {
            removeObserver(this)
        }
        downloadObFunMap[downloadOb.cancelFunc]?.run {
            removeObserver(this)
        }
        downloadObFunMap[downloadOb.failFunc]?.run {
            removeObserver(this)
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
        Log.e(Downloader.logTagObject, Downloader.TAG, error.toString())
        taskFail(DownloadId(false, download.id), download)
    }

    override fun onError(
        groupId: Int,
        download: Download,
        error: Error,
        throwable: Throwable?,
        fetchGroup: FetchGroup
    ) {
        Log.e(Downloader.logTagObject, Downloader.TAG, error.toString())
        taskFail(DownloadId(true, groupId), download)
    }

    private fun taskStart(id: DownloadId, download: Download) {
        notifyChanged(id.getStartNotifyKey(), download)
    }

    private fun taskRunning(id: DownloadId, download: Download) {
        notifyChanged(id.getRunningNotifyKey(), download)
    }

    private fun taskStop(id: DownloadId, download: Download) {
        notifyChanged(id.getStopNotifyKey(), download)
    }

    private fun taskComplete(id: DownloadId, download: Download) {
        notifyChanged(id.getCompleteNotifyKey(), download)
    }

    private fun taskCancel(id: DownloadId, download: Download) {
        notifyChanged(id.getCancelNotifyKey(), download)
    }

    private fun taskFail(id: DownloadId, download: Download) {
        notifyChanged(id.getFailNotifyKey(), download)
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
