package net.xzos.upgradeall.ui.applist.base.star

import net.xzos.upgradeall.ui.applist.base.TAB_STAR
import net.xzos.upgradeall.ui.applist.base.normal.NormalAppHubListAdapter
import net.xzos.upgradeall.ui.applist.base.normal.NormalAppHubListFragment


class StarAppHubListFragment(appType: String) : NormalAppHubListFragment(appType, TAB_STAR) {
    override val adapter = NormalAppHubListAdapter()
}