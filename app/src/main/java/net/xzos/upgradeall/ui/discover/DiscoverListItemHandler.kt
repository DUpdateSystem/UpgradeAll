package net.xzos.upgradeall.ui.discover

import androidx.fragment.app.FragmentManager
import net.xzos.upgradeall.ui.applist.base.AppHubListItemHandler
import net.xzos.upgradeall.utils.runUiFun

class DiscoverListItemHandler(
        private val viewModel: DiscoverViewModel,
        private val supportFragmentManager: FragmentManager,
) : AppHubListItemHandler() {
    fun onClickDiscover(uuid: String) {
        ConfigDownloadDialog(uuid) {
            runUiFun { viewModel.loadData() }
        }.show(supportFragmentManager)
    }
}