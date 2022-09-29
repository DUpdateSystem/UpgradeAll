package net.xzos.upgradeall.wrapper.download

import net.xzos.upgradeall.core.downloader.filedownloader.item.Status
import net.xzos.upgradeall.core.downloader.filedownloader.item.TaskSnap
import net.xzos.upgradeall.core.downloader.filedownloader.item.progress
import net.xzos.upgradeall.core.utils.oberver.Tag

enum class DownloadTaskerStatus(val msg: String? = null) : Tag {
    NONE,
    INFO_RENEW("get download info"),
    INFO_COMPLETE("download info acquired"),
    INFO_FAILED("fail get download info"),
    WAIT_START("wait task start"),
    STARTED("task started"),
    IN_DOWNLOAD("task content in downloader"),
    START_FAIL("task start failed"),
    EXTERNAL_DOWNLOAD("download external"),

    // 下载状态包装
    DOWNLOAD_START,
    DOWNLOAD_RUNNING,
    DOWNLOAD_STOP,
    DOWNLOAD_COMPLETE,
    DOWNLOAD_CANCEL,
    DOWNLOAD_FAIL
}

fun Status.taskStatus() = when (this) {
    Status.START -> DownloadTaskerStatus.DOWNLOAD_START
    Status.RUNNING -> DownloadTaskerStatus.DOWNLOAD_RUNNING
    Status.STOP -> DownloadTaskerStatus.DOWNLOAD_STOP
    Status.COMPLETE -> DownloadTaskerStatus.DOWNLOAD_COMPLETE
    Status.CANCEL -> DownloadTaskerStatus.DOWNLOAD_CANCEL
    Status.FAIL -> DownloadTaskerStatus.DOWNLOAD_FAIL
    else -> DownloadTaskerStatus.NONE
}

class DownloadTaskerSnap(
    val status: DownloadTaskerStatus,
    val snapList: List<TaskSnap>,
) {
    var error: Throwable? = null
    var msg: String = ""
    fun error(error: Throwable) = this.apply {
        this.error = error
    }

    fun msg(msg: String): DownloadTaskerSnap = this.apply {
        this.msg = msg
    }
}

fun DownloadTaskerSnap?.status() = this?.status ?: DownloadTaskerStatus.NONE

fun DownloadTaskerSnap.progress(): Float {
    var downloadSize: Long = 0
    var totalSize: Long = 0
    snapList.forEach {
        downloadSize += it.downloadSize
        totalSize += it.totalSize
    }
    return TaskSnap(Status.NONE, downloadSize, totalSize).progress()
}

fun DownloadTaskerSnap.speed(): Long {
    var speed = 0L
    snapList.forEach {
        speed += it.speed
    }
    return speed
}

val DownloadInfoEmpty = RuntimeException("DownloadInfoEmpty")
