package net.xzos.upgradeall.core.downloader

enum class DownloadStatus {

    /** Indicates when a download is newly created and not yet queued.*/
    NONE,

    /** Indicates when a newly created download is queued.*/
    QUEUED,

    /** Indicates when a download is currently being downloaded.*/
    DOWNLOADING,

    /** Indicates when a download is paused.*/
    PAUSED,

    /** Indicates when a download is completed.*/
    COMPLETED,

    /** Indicates when a download is cancelled.*/
    CANCELLED,

    /** Indicates when a download has failed.*/
    FAILED,

    /** Indicates when a download has been removed and is no longer managed by Fetch.*/
    REMOVED,

    /** Indicates when a download has been deleted and is no longer managed by Fetch.*/
    DELETED,

    /** Indicates when a download has been Added to Fetch for management.*/
    ADDED;

    companion object {

        fun valueOf(value: Int): DownloadStatus {
            return when (value) {
                0 -> NONE
                1 -> QUEUED
                2 -> DOWNLOADING
                3 -> PAUSED
                4 -> COMPLETED
                5 -> CANCELLED
                6 -> FAILED
                7 -> REMOVED
                8 -> DELETED
                9 -> ADDED
                else -> NONE
            }
        }
    }
}