package net.xzos.upgradeall.ui.applist.base.update

import androidx.fragment.app.viewModels
import net.xzos.upgradeall.ui.applist.base.AppHubListFragment


class UpdateAppHubListFragment(appType: String) : AppHubListFragment<UpdateAppListItemView, UpdateAppHubListViewHolder>(appType) {

    override val adapter = UpdateAppHubListAdapter()
    override val viewModel by viewModels<UpdateAppHubListViewModel>()
}