package net.xzos.upgradeall.core.downloader.filedownloader.observe

import net.xzos.upgradeall.core.downloader.filedownloader.item.Downloader
import net.xzos.upgradeall.core.downloader.filedownloader.item.Status
import net.xzos.upgradeall.core.utils.coroutines.coroutinesMutableMapOf
import net.xzos.upgradeall.core.utils.oberver.Informer

internal object DownloadRegister {

    private val informerMap = coroutinesMutableMapOf<Downloader, Informer>(true)

    private fun newDownloadInformer(): Informer {
        return object : Informer {
            override val informerId = Informer.getInformerId()
        }
    }

    internal fun registerDownloader(downloader: Downloader) {
        val taskList = downloader.getTaskList()
        taskList.forEach { taskWrapper ->
            taskWrapper.subscribe {
                when (val downloaderStatus = downloader.status) {
                    Status.NONE -> {}
                    Status.START -> {
                        taskStart(downloader, downloaderStatus)
                    }
                    Status.RUNNING -> {
                        taskRunning(downloader, downloaderStatus)
                    }
                    Status.STOP -> {
                        taskStop(downloader, downloaderStatus)
                    }
                    Status.COMPLETE -> {
                        taskComplete(downloader, downloaderStatus)
                    }
                    Status.FAIL -> {
                        taskFail(downloader, downloaderStatus)
                    }
                    Status.CANCEL -> {
                        taskCancel(downloader, downloaderStatus)
                    }
                }
            }
        }
    }

    fun registerOb(downloader: Downloader, downloadOb: DownloadOb) {
        with(informerMap.getOrDefault(downloader) { newDownloadInformer() }) {
            observeForever(Status.START, downloadOb.startFunc)
            observeForever(Status.RUNNING, downloadOb.runningFunc)
            observeForever(Status.STOP, downloadOb.stopFunc)
            observeForever(Status.COMPLETE, downloadOb.completeFunc)
            observeForever(Status.CANCEL, downloadOb.cancelFunc)
            observeForever(Status.FAIL, downloadOb.failFunc)
        }
    }

    fun unRegisterId(downloader: Downloader) {
        informerMap.remove(downloader)
    }

    fun unRegisterOb(downloader: Downloader, downloadOb: DownloadOb) {
        informerMap[downloader]?.run {
            removeObserver(downloadOb.startFunc)
            removeObserver(downloadOb.runningFunc)
            removeObserver(downloadOb.stopFunc)
            removeObserver(downloadOb.completeFunc)
            removeObserver(downloadOb.cancelFunc)
            removeObserver(downloadOb.failFunc)
        }
    }

    private fun taskStart(id: Downloader, status: Status) {
        informerMap[id]?.run {
            notifyChanged(Status.START, status)
        }
    }

    private fun taskRunning(downloader: Downloader, status: Status) {
        informerMap[downloader]?.run {
            notifyChanged(Status.RUNNING, status)
        }
    }

    private fun taskStop(downloader: Downloader, status: Status) {
        informerMap[downloader]?.run {
            notifyChanged(Status.STOP, status)
        }
    }

    private fun taskComplete(downloader: Downloader, status: Status) {
        informerMap[downloader]?.run {
            notifyChanged(Status.COMPLETE, status)
        }
    }

    private fun taskCancel(downloader: Downloader, status: Status) {
        informerMap[downloader]?.run {
            notifyChanged(Status.CANCEL, status)
        }
    }

    private fun taskFail(downloader: Downloader, status: Status) {
        informerMap[downloader]?.run {
            notifyChanged(Status.FAIL, status)
        }
    }
}