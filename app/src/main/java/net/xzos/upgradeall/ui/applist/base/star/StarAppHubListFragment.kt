package net.xzos.upgradeall.ui.applist.base.star

import androidx.fragment.app.viewModels
import net.xzos.upgradeall.ui.applist.base.AppHubListFragment
import net.xzos.upgradeall.ui.applist.base.normal.NormalAppHubListAdapter
import net.xzos.upgradeall.ui.applist.base.normal.NormalAppHubListFragment
import net.xzos.upgradeall.ui.applist.base.normal.NormalAppHubListViewHolder
import net.xzos.upgradeall.ui.applist.base.normal.NormalAppListItemView


class StarAppHubListFragment(appType: String) : NormalAppHubListFragment(appType) {
    override val adapter = NormalAppHubListAdapter()
    override val viewModel by viewModels<StarAppHubListViewModel>()
}