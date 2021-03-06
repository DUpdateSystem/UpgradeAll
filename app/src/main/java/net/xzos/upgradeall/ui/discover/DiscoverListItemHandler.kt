package net.xzos.upgradeall.ui.discover

import androidx.fragment.app.FragmentManager
import net.xzos.upgradeall.ui.base.recycleview.RecyclerViewHandler

class DiscoverListItemHandler(
        private val viewModel: DiscoverViewModel,
        private val supportFragmentManager: FragmentManager,
) : RecyclerViewHandler() {
    fun onClickDiscover(uuid: String) {
        ConfigDownloadDialog(uuid) {
            viewModel.loadData()
        }.show(supportFragmentManager)
    }
}