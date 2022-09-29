package net.xzos.upgradeall.core.downloader.filedownloader.item

import net.xzos.upgradeall.core.downloader.filedownloader.DownloadCanceledError
import net.xzos.upgradeall.core.downloader.filedownloader.getDownloadDir
import net.xzos.upgradeall.core.downloader.filedownloader.item.data.InputData
import net.xzos.upgradeall.core.downloader.filedownloader.item.data.TaskData
import net.xzos.upgradeall.core.downloader.filedownloader.item.data.getTaskData
import net.xzos.upgradeall.core.downloader.filedownloader.item.data.manager
import net.xzos.upgradeall.core.utils.log.ObjectTag
import net.xzos.upgradeall.core.utils.log.ObjectTag.Companion.core
import net.xzos.upgradeall.core.utils.oberver.InformerNoTag
import java.io.File


/* 下载管理 */
class Downloader internal constructor(
    downloadDir: File,
) : InformerNoTag<Status>() {

    val id by lazy { hashCode() }

    private val downloadFile by lazy { getDownloadDir(downloadDir) }

    private val requestList: MutableList<TaskData> = mutableListOf()
    private val taskList: MutableList<TaskWrapper> = mutableListOf()

    private fun notifyStatus() {
        notifyChanged(status())
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
            val status = it.snap
            totalSize += status.totalSize
            downloadedSize += status.downloadSize
        }
        return downloadedSize / totalSize * 100
    }

    fun getTaskList(): List<TaskWrapper> {
        return taskList
    }

    fun status(): Status {
        return getTaskList().run {
            when {
                isEmpty() -> Status.NONE
                any { it.snap.status == Status.START } -> Status.START
                any { it.snap.status == Status.RUNNING } -> Status.RUNNING
                all { it.snap.status == Status.STOP } -> Status.STOP
                all { it.snap.status == Status.COMPLETE } -> Status.COMPLETE
                all { it.snap.status == Status.CANCEL } -> Status.CANCEL
                any { it.snap.status == Status.FAIL } -> Status.FAIL
                else -> Status.NONE
            }
        }
    }

    private suspend fun TaskData.managerWrapper() = this.manager().wrapper({ notifyStatus() })

    private fun TaskWrapper.start() = this.apply { manager.start() }

    suspend fun start(
        taskStartedFun: () -> Unit,
        taskStartFailedFun: (Throwable) -> Unit,
    ) {
        if (requestList.isEmpty()) {
            taskStartFailedFun(DownloadCanceledError("no request list"))
            return
        }
        taskList.addAll(requestList.map { it.managerWrapper().start() })
        requestList.clear()
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
    }

    companion object {
        const val TAG = "Downloader"
        val logTagObject = ObjectTag(core, TAG)
    }
}