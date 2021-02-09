package net.xzos.upgradeall.ui.discover

import androidx.activity.viewModels
import net.xzos.upgradeall.ui.base.list.HubListActivity


class DiscoverActivity : HubListActivity<DiscoverListItemView, DiscoverListViewHolder>() {

    override val viewModel by viewModels<DiscoverViewModel>()
    override val adapter by lazy { DiscoveryAdapter(DiscoverListItemHandler(viewModel, supportFragmentManager)) }
}