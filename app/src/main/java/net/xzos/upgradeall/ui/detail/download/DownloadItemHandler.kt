package net.xzos.upgradeall.ui.detail.download

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.xzos.upgradeall.core.module.app.FileAsset
import net.xzos.upgradeall.ui.base.recycleview.RecyclerViewHandler
import net.xzos.upgradeall.ui.detail.AppDetailViewModel

class DownloadItemHandler(private val appDetailViewModel: AppDetailViewModel) : RecyclerViewHandler() {
    fun clickDownload(fileAsset: FileAsset) {
        GlobalScope.launch {
            appDetailViewModel.download(fileAsset)
        }
    }
}