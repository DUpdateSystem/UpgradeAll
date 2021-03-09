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

const val EXTRA_APP_TYPE = "EXTRA_APP_TYPE"
const val EXTRA_TAB_INDEX = "EXTRA_TAB_INDEX"

fun getAppHubListFragment(appType: String, tabIndex: Int): Fragment {
    val bundle = Bundle().apply {
        putString(EXTRA_APP_TYPE, appType)
        putInt(EXTRA_TAB_INDEX, tabIndex)
    }
    return when (tabIndex) {
        TAB_UPDATE -> UpdateAppHubListFragment().apply { arguments = bundle }
        TAB_STAR -> StarAppHubListFragment().apply { arguments = bundle }
        else -> NormalAppHubListFragment().apply { arguments = bundle }
    }
}

abstract class AppHubListFragment<L : BaseAppListItemView, LV : RecyclerViewHolder<L, *, ItemHubAppBinding>> : HubListFragment<App, L, LV>() {
    private val appType by lazy { arguments?.getString(EXTRA_APP_TYPE) ?: throw IllegalArgumentException("appType is null") }
    private val tabIndex by lazy { arguments?.getInt(EXTRA_TAB_INDEX) ?: throw IllegalArgumentException("tabIndex is null") }

    override val viewModel by viewModels<AppHubViewModel>()
    override fun onCreate(savedInstanceState: Bundle?) {
        viewModel.initSetting(appType, tabIndex)
        super.onCreate(savedInstanceState)
    }
}