package net.xzos.upgradeall.ui.detail.download

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.xzos.upgradeall.core.module.app.version.AssetWrapper
import net.xzos.upgradeall.ui.base.recycleview.RecyclerViewHandler
import net.xzos.upgradeall.ui.detail.AppDetailViewModel

class DownloadItemHandler(private val appDetailViewModel: AppDetailViewModel) :
    RecyclerViewHandler() {
    fun clickDownload(fileAsset: AssetWrapper) {
        GlobalScope.launch {
            appDetailViewModel.download(fileAsset, false)
        }
    }

    fun longClickDownload(fileAsset: AssetWrapper): Boolean {
        GlobalScope.launch {
            appDetailViewModel.download(fileAsset, true)
        }
        return true
    }
}