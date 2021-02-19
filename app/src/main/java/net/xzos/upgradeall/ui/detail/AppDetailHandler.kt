package net.xzos.upgradeall.ui.detail

import androidx.fragment.app.FragmentManager
import net.xzos.upgradeall.ui.detail.download.DownloadListDialog

class AppDetailHandler(
        private val viewModel: AppDetailViewModel,
        private val supportFragmentManager: FragmentManager,
) {
    fun clickDownload() {
        DownloadListDialog(viewModel.currentVersion?.assetList
                ?: return, viewModel).show(supportFragmentManager)
    }
}