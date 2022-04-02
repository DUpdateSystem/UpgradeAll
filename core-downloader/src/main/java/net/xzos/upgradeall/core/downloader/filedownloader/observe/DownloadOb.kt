package net.xzos.upgradeall.core.downloader.filedownloader.observe

import net.xzos.upgradeall.core.downloader.filedownloader.item.Status

class DownloadOb(
    internal val startFunc: (Status) -> Unit,
    internal val runningFunc: (Status) -> Unit,
    internal val stopFunc: (Status) -> Unit,
    internal val completeFunc: (Status) -> Unit,
    internal val cancelFunc: (Status) -> Unit,
    internal val failFunc: (Status) -> Unit,
)