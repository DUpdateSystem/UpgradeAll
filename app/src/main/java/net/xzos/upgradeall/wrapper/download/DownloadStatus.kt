package net.xzos.upgradeall.wrapper.download

import net.xzos.upgradeall.core.utils.oberver.Tag

enum class DownloadStatus(val msg: String? = null) : Tag {
    DOWNLOAD_INFO_RENEW("get download info"),
    DOWNLOAD_INFO_COMPLETE("download info acquired"),
    DOWNLOAD_INFO_FAILED("fail get download info"),
    TASK_WAIT_START("wait task start"),
    TASK_STARTED("task started"),
    TASK_START_FAIL("task start failed"),
    EXTERNAL_DOWNLOAD("download external"),
}

val DownloadInfoEmpty = RuntimeException("DownloadInfoEmpty")
