package net.xzos.upgradeall.ui.applist.base.applications

import net.xzos.upgradeall.ui.applist.base.normal.NormalAppHubListAdapter
import net.xzos.upgradeall.ui.applist.base.normal.NormalAppHubListFragment


class ApplicationsAppHubListFragment : NormalAppHubListFragment() {
    override val adapter = NormalAppHubListAdapter()
}