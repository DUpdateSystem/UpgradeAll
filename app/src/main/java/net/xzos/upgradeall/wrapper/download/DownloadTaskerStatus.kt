package net.xzos.upgradeall.wrapper.download

import net.xzos.upgradeall.core.downloader.filedownloader.item.DownloadStatusSnap
import net.xzos.upgradeall.core.utils.oberver.Tag

enum class DownloadTaskerStatus(val msg: String? = null) : Tag {
    INFO_RENEW("get download info"),
    INFO_COMPLETE("download info acquired"),
    INFO_FAILED("fail get download info"),
    WAIT_START("wait task start"),
    STARTED("task started"),
    IN_DOWNLOAD("task content in downloader"),
    START_FAIL("task start failed"),
    EXTERNAL_DOWNLOAD("download external"),
}

class DownloadTaskerSnap(
    val status: DownloadTaskerStatus,
    val snapList: List<DownloadStatusSnap>,
) {
    var error: Throwable? = null
    var msg: String = ""
    fun error(error: Throwable) {
        this.error = error
    }

    fun msg(msg: String) {
        this.msg = msg
    }
}

fun DownloadTaskerSnap.getDownloadStatus() = snapList.map { it.status }

fun DownloadTaskerSnap.progress(): Long {
    var total = 0L
    var download = 0L
    snapList.forEach {
        total += it.totalSize
        download += it.downloadSize
    }
    return download / total
}

fun DownloadTaskerSnap.speed(): Long {
    var speed = 0L
    snapList.forEach {
        speed += it.speed
    }
    return speed
}

val DownloadInfoEmpty = RuntimeException("DownloadInfoEmpty")
