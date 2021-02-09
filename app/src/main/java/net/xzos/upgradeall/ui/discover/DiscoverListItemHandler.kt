package net.xzos.upgradeall.ui.discover

import androidx.fragment.app.FragmentManager
import net.xzos.upgradeall.ui.base.recycleview.RecyclerViewHandler
import net.xzos.upgradeall.utils.runUiFun

class DiscoverListItemHandler(
        private val viewModel: DiscoverViewModel,
        private val supportFragmentManager: FragmentManager,
) : RecyclerViewHandler() {
    fun onClickDiscover(uuid: String) {
        ConfigDownloadDialog(uuid) {
            runUiFun { viewModel.loadData() }
        }.show(supportFragmentManager)
    }
}