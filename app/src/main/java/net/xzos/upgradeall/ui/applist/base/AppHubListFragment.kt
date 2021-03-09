package net.xzos.upgradeall.ui.applist.base

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import net.xzos.upgradeall.core.module.app.App
import net.xzos.upgradeall.databinding.ItemHubAppBinding
import net.xzos.upgradeall.ui.applist.base.normal.NormalAppHubListFragment
import net.xzos.upgradeall.ui.applist.base.star.StarAppHubListFragment
import net.xzos.upgradeall.ui.applist.base.update.UpdateAppHubListFragment
import net.xzos.upgradeall.ui.base.list.HubListFragment
import net.xzos.upgradeall.ui.base.recycleview.RecyclerViewHolder


fun getAppHubListFragment(appType: String, tabIndex: Int): Fragment {
    return when (tabIndex) {
        TAB_UPDATE -> UpdateAppHubListFragment(appType)
        TAB_STAR -> StarAppHubListFragment(appType)
        else -> NormalAppHubListFragment(appType, tabIndex)
    }
}

abstract class AppHubListFragment<L : BaseAppListItemView, LV : RecyclerViewHolder<L, *, ItemHubAppBinding>>(
        private val appType: String, private val tabIndex: Int
) : HubListFragment<App, L, LV>() {
    override val viewModel by viewModels<AppHubViewModel>()
    override fun onCreate(savedInstanceState: Bundle?) {
        viewModel.initSetting(appType, tabIndex)
        super.onCreate(savedInstanceState)
    }
}