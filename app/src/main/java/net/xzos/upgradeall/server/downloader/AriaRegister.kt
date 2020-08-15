package net.xzos.upgradeall.server.downloader

import com.arialyy.annotations.Download
import com.arialyy.aria.core.Aria
import com.arialyy.aria.core.task.DownloadTask
import net.xzos.upgradeall.core.oberver.Informer

object AriaRegister : Informer {

    private const val TASK_START = "TASK_START"
    private const val TASK_RUNNING = "TASK_RUNNING"
    private const val TASK_STOP = "TASK_STOP"
    private const val TASK_COMPLETE = "TASK_COMPLETE"
    private const val TASK_CANCEL = "TASK_CANCEL"
    private const val TASK_FAIL = "TASK_FAIL"

    internal fun String.getStartNotifyKey(): String = this + TASK_START
    internal fun String.getRunningNotifyKey(): String = this + TASK_RUNNING
    internal fun String.getStopNotifyKey(): String = this + TASK_STOP
    internal fun String.getCompleteNotifyKey(): String = this + TASK_COMPLETE
    internal fun String.getCancelNotifyKey(): String = this + TASK_CANCEL
    internal fun String.getFailNotifyKey(): String = this + TASK_FAIL

    init {
        Aria.download(this).register()
    }

    @Download.onTaskStart
    fun taskStart(task: DownloadTask) {
        notifyChanged(task.key.getStartNotifyKey(), task)
    }

    @Download.onTaskResume
    @Download.onTaskRunning
    fun taskRunning(task: DownloadTask) {
        notifyChanged(task.key.getRunningNotifyKey(), task)
    }

    @Download.onTaskStop
    fun taskStop(task: DownloadTask) {
        notifyChanged(task.key.getStopNotifyKey(), task)
    }

    @Download.onTaskComplete
    fun taskComplete(task: DownloadTask) {
        notifyChanged(task.key.getCompleteNotifyKey(), task)
    }

    @Download.onTaskCancel
    fun taskCancel(task: DownloadTask) {
        notifyChanged(task.key.getCancelNotifyKey(), task)
    }

    @Download.onTaskFail
    fun taskFail(task: DownloadTask?) {
        task ?: return
        notifyChanged(task.key.getFailNotifyKey(), task)
    }
}
