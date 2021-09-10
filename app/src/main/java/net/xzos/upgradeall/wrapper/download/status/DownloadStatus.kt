package net.xzos.upgradeall.wrapper.download.status

import net.xzos.upgradeall.core.utils.oberver.Tag

enum class DownloadStatus(val msg: String? = null) : Tag {
    DOWNLOAD_WAIT_INFO("wait download info"),
    TASK_WAIT_START("wait task start"),
    TASK_STARTED("task started"),
    TASK_START_FAIL("task start failed"),
    EXTERNAL_DOWNLOAD("download external"),

    // downloading status
    DOWNLOAD_START,
    DOWNLOADING,
    DOWNLOAD_STOP,
    DOWNLOAD_COMPLETE,
    DOWNLOAD_CANCEL,
    DOWNLOAD_FAIL,
}

val DownloadInfoEmpty = RuntimeException("DownloadInfoEmpty")
