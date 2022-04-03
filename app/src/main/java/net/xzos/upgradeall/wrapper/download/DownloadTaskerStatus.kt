package net.xzos.upgradeall.wrapper.download

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

fun DownloadTaskerSnap.progress(): Long {
    var progress = 0L
    snapList.forEach {
        progress += it.progress()
    }
    return if (progress == 0L) 0
    else progress / snapList.size
}

fun DownloadTaskerSnap.speed(): Long {
    var speed = 0L
    snapList.forEach {
        speed += it.speed
    }
    return speed
}

val DownloadInfoEmpty = RuntimeException("DownloadInfoEmpty")
