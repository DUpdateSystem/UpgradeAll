package net.xzos.upgradeall.core.downloader.filetasker

import net.xzos.upgradeall.core.utils.oberver.Tag

enum class FileTaskerStatus : Tag {
    NONE,
    DOWNLOAD_NOT_SET,
    DOWNLOAD_QUEUE,
    DOWNLOAD_START_FAIL,
    DOWNLOAD_START,
    DOWNLOAD_RUNNING,
    DOWNLOAD_STOP,
    DOWNLOAD_COMPLETE,
    DOWNLOAD_CANCEL,
    DOWNLOAD_FAIL,
}

