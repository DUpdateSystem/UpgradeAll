package net.xzos.upgradeall.ui.applist.base.normal

import net.xzos.upgradeall.ui.applist.base.AppHubListFragment


open class NormalAppHubListFragment
    : AppHubListFragment<NormalAppListItemView, NormalAppHubListViewHolder>() {
    override val adapter = NormalAppHubListAdapter(
            listContainerViewConvertFun = {
                NormalAppListItemView(it).apply { renew(requireContext()) }
            })
}