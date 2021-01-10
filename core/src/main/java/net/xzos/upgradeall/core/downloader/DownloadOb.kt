package net.xzos.upgradeall.core.downloader

import com.tonyodev.fetch2.Download
import net.xzos.upgradeall.core.utils.FuncR

class DownloadOb(
        internal val startFunc: FuncR<Download>,
        internal val runningFunc: FuncR<Download>,
        internal val stopFunc: FuncR<Download>,
        internal val completeFunc: FuncR<Download>,
        internal val cancelFunc: FuncR<Download>,
        internal val failFunc: FuncR<Download>,
)