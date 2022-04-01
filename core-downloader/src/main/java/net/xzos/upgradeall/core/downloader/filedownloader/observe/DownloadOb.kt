package net.xzos.upgradeall.core.downloader.filedownloader.observe

import net.xzos.upgradeall.core.downloader.filedownloader.item.DownloadStatusSnap

class DownloadOb(
    internal val startFunc: (List<DownloadStatusSnap>) -> Unit,
    internal val runningFunc: (List<DownloadStatusSnap>) -> Unit,
    internal val stopFunc: (List<DownloadStatusSnap>) -> Unit,
    internal val completeFunc: (List<DownloadStatusSnap>) -> Unit,
    internal val cancelFunc: (List<DownloadStatusSnap>) -> Unit,
    internal val failFunc: (List<DownloadStatusSnap>) -> Unit,
) {
    companion object {
        fun getEmptyDownloadOb() = DownloadOb({}, {}, {}, {}, {}, {})
    }
}