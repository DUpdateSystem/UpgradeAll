package net.xzos.upgradeall.core.downloader.filedownloader.item

import net.xzos.upgradeall.core.downloader.filedownloader.DownloadCanceledError
import net.xzos.upgradeall.core.downloader.filedownloader.DownloaderManager
import net.xzos.upgradeall.core.downloader.filedownloader.observe.DownloadOb
import net.xzos.upgradeall.core.downloader.filedownloader.observe.DownloadRegister
import net.xzos.upgradeall.core.utils.log.ObjectTag
import net.xzos.upgradeall.core.utils.log.ObjectTag.Companion.core
import zlc.season.rxdownload4.manager.delete
import zlc.season.rxdownload4.manager.start
import zlc.season.rxdownload4.manager.stop
import java.io.File


/* 下载管理 */
class Downloader(downloadDir: File) {
    val id by lazy { hashCode() }

    var status: Status = Status.NONE
        private set

    private fun renewStatus() {
        val taskList = taskList
        status = when {
            taskList.isEmpty() -> Status.NONE
            taskList.any { it.snap?.status == Status.START } -> Status.START
            taskList.any { it.snap?.status == Status.RUNNING } -> Status.RUNNING
            taskList.all { it.snap?.status == Status.STOP } -> Status.RUNNING
            taskList.all { it.snap?.status == Status.COMPLETE } -> Status.COMPLETE
            taskList.all { it.snap?.status == Status.CANCEL } -> Status.CANCEL
            taskList.any { it.snap?.status == Status.FAIL } -> Status.FAIL
            else -> Status.NONE
        }
    }

    private val downloadFile by lazy { getDownloadDir(downloadDir) }

    private val requestList: MutableList<TaskData> = mutableListOf()
    private val taskList: MutableList<TaskWrapper> = mutableListOf()

    fun register(downloadOb: DownloadOb) {
        DownloadRegister.registerOb(this, downloadOb)
    }

    fun unregister(downloadOb: DownloadOb) {
        DownloadRegister.unRegisterOb(this, downloadOb)
    }

    fun removeFile() {
        delTask()
        downloadFile.delete()
    }

    fun addTask(inputData: InputData) {
        requestList.add(inputData.getTaskData(downloadFile))
    }

    fun getDownloadProgress(): Long {
        var totalSize = 0L
        var downloadedSize = 0L
        taskList.forEach {
            val status = it.snap ?: return@forEach
            totalSize += status.totalSize
            downloadedSize += status.downloadSize
        }
        return downloadedSize / totalSize * 100
    }

    fun getTaskList(): List<TaskWrapper> {
        return taskList
    }

    fun TaskWrapper.start() = this.apply {
        subscribe { renewStatus() }
        manager.start()
    }

    fun start(
        taskStartedFun: () -> Unit,
        taskStartFailedFun: (Throwable) -> Unit,
        vararg downloadOb: DownloadOb
    ) {
        if (requestList.isEmpty()) {
            taskStartFailedFun(DownloadCanceledError("no request list"))
            return
        }
        taskList.addAll(requestList.map { it.manager().wrapper().start() })
        requestList.clear()
        register(*downloadOb)
        taskStartedFun()
    }

    fun resume(tasker: TaskWrapper? = null) {
        tasker?.also { it.manager.start() }
            ?: taskList.forEach { it.manager.start() }
    }

    fun pause(tasker: TaskWrapper? = null) {
        tasker?.also { it.manager.stop() }
            ?: taskList.forEach { it.manager.stop() }
    }

    fun retry(tasker: TaskWrapper? = null) {
        tasker?.also { it.manager.start() }
            ?: taskList.forEach { it.manager.start() }
    }

    fun cancel(tasker: TaskWrapper? = null) {
        tasker?.also { it.manager.delete() }
            ?: taskList.forEach { it.manager.delete() }
    }

    private fun delTask() {
        taskList.forEach { it.manager.delete() }
        downloadFile.delete()
        unregister()
    }

    private fun register(vararg downloadOb: DownloadOb) {
        DownloaderManager.addDownloader(this)
        downloadOb.forEach {
            DownloadRegister.registerOb(this, it)
        }
    }

    private fun unregister() {
        DownloaderManager.removeDownloader(this)
    }

    companion object {
        const val TAG = "Downloader"
        val logTagObject = ObjectTag(core, TAG)
    }
}