package net.xzos.upgradeall.ui.applist.base

import androidx.fragment.app.Fragment
import net.xzos.upgradeall.core.module.app.App
import net.xzos.upgradeall.ui.applist.base.normal.NormalAppHubListFragment
import net.xzos.upgradeall.ui.applist.base.star.StarAppHubListFragment
import net.xzos.upgradeall.ui.applist.base.update.UpdateAppHubListFragment
import net.xzos.upgradeall.ui.base.list.HubListFragment
import net.xzos.upgradeall.ui.base.recycleview.RecyclerViewHolder


fun getAppHubListFragment(index: Int, viewModel: AppHubViewModel): Fragment {
    return when (index) {
        0 -> UpdateAppHubListFragment(viewModel)
        1 -> StarAppHubListFragment(viewModel)
        else -> NormalAppHubListFragment(viewModel)
    }
}

abstract class AppHubListFragment<L : BaseAppListItemView, LV : RecyclerViewHolder<L>>(
        override val viewModel: AppHubViewModel, private val tabIndex: Int
) : HubListFragment<App, L, LV>() {
    override fun onResume() {
        super.onResume()
        viewModel.setTabIndex(tabIndex)
    }
}