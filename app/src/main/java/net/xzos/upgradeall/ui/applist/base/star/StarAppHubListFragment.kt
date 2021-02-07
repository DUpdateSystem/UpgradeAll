package net.xzos.upgradeall.ui.applist.base.star

import net.xzos.upgradeall.ui.applist.base.AppHubViewModel
import net.xzos.upgradeall.ui.applist.base.TAB_STAR
import net.xzos.upgradeall.ui.applist.base.normal.NormalAppHubListAdapter
import net.xzos.upgradeall.ui.applist.base.normal.NormalAppHubListFragment


class StarAppHubListFragment(viewModel: AppHubViewModel) : NormalAppHubListFragment(viewModel, TAB_STAR) {
    override val adapter = NormalAppHubListAdapter()
}