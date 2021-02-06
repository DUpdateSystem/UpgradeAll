package net.xzos.upgradeall.ui.applist.base.star

import net.xzos.upgradeall.ui.applist.base.AppHubViewModel
import net.xzos.upgradeall.ui.applist.base.normal.NormalAppHubListAdapter
import net.xzos.upgradeall.ui.applist.base.normal.NormalAppHubListFragment


class StarAppHubListFragment(viewModel: AppHubViewModel) : NormalAppHubListFragment(viewModel, STAR_TAB) {
    override val adapter = NormalAppHubListAdapter()
}