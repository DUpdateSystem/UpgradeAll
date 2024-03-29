package net.xzos.upgradeall.ui.applist.base

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import net.xzos.upgradeall.core.module.app.App
import net.xzos.upgradeall.databinding.ItemHubAppBinding
import net.xzos.upgradeall.ui.applist.base.applications.ApplicationsAppHubListFragment
import net.xzos.upgradeall.ui.applist.base.normal.NormalAppHubListFragment
import net.xzos.upgradeall.ui.applist.base.star.StarAppHubListFragment
import net.xzos.upgradeall.ui.applist.base.update.UpdateAppHubListFragment
import net.xzos.upgradeall.ui.base.list.HubListFragment
import net.xzos.upgradeall.ui.base.recycleview.RecyclerViewHolder

const val EXTRA_APP_TYPE = "EXTRA_APP_TYPE"
const val EXTRA_TAB_INDEX = "EXTRA_TAB_INDEX"

fun getAppHubListFragment(appType: String, tabIndex: TabIndex): Fragment {
    val bundle = Bundle().apply {
        putString(EXTRA_APP_TYPE, appType)
        putString(EXTRA_TAB_INDEX, tabIndex.tag)
    }
    return when (tabIndex) {
        TabIndex.TAB_UPDATE -> UpdateAppHubListFragment()
        TabIndex.TAB_STAR -> StarAppHubListFragment()
        TabIndex.TAB_ALL -> NormalAppHubListFragment()
        else -> ApplicationsAppHubListFragment()
    }.apply { setArguments(bundle) }
}

abstract class AppHubListFragment<L : BaseAppListItemView, LV : RecyclerViewHolder<L, *, ItemHubAppBinding>> :
    HubListFragment<App, L, LV>() {
    private val appType by lazy {
        arguments?.getString(EXTRA_APP_TYPE) ?: throw IllegalArgumentException("appType is null")
    }
    private val tabIndex by lazy {
        TabIndex.valueOf(
            arguments?.getString(EXTRA_TAB_INDEX)
                ?: throw IllegalArgumentException("tabIndex is null")
        )
    }

    override val viewModel by viewModels<AppHubViewModel>()
    override fun onCreate(savedInstanceState: Bundle?) {
        viewModel.initData(appType, tabIndex)
        super.onCreate(savedInstanceState)
        adapter.setHasStableIds(true)
    }
}