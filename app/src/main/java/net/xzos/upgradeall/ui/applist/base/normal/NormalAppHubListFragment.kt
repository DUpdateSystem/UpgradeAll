package net.xzos.upgradeall.ui.applist.base.normal

import androidx.fragment.app.viewModels
import net.xzos.upgradeall.ui.applist.base.AppHubListFragment


open class NormalAppHubListFragment(appType: String) : AppHubListFragment<NormalAppListItemView, NormalAppHubListViewHolder>(appType) {
    override val adapter = NormalAppHubListAdapter()
    override val viewModel by viewModels<NormalAppHubListViewModel>()
}