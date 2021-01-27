package net.xzos.upgradeall.core.downloader

import com.tonyodev.fetch2.Download

class DownloadOb(
        internal val startFunc: (Download) -> Unit,
        internal val runningFunc: (Download) -> Unit,
        internal val stopFunc: (Download) -> Unit,
        internal val completeFunc: (Download) -> Unit,
        internal val cancelFunc: (Download) -> Unit,
        internal val failFunc: (Download) -> Unit,
)