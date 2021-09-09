package net.xzos.upgradeall.wrapper.download.status

import net.xzos.upgradeall.core.utils.oberver.Tag

enum class DownloadStatus : Tag {
    DOWNLOAD_WAIT_INFO, TASK_WAIT_START, TASK_START_FAIL,
    EXTERNAL_DOWNLOAD,

    // downloading status
    DOWNLOAD_START,
    DOWNLOADING,
    DOWNLOAD_STOP,
    DOWNLOAD_COMPLETE,
    DOWNLOAD_CANCEL,
    DOWNLOAD_FAIL,
}

val DownloadInfoEmpty = RuntimeException("DownloadInfoEmpty")
