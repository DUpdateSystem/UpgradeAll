package net.xzos.upgradeall.ui.applist.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import net.xzos.upgradeall.ui.applist.base.normal.NormalAppHubListFragment
import net.xzos.upgradeall.ui.applist.base.star.StarAppHubListFragment
import net.xzos.upgradeall.ui.applist.base.update.UpdateAppHubListFragment
import net.xzos.upgradeall.ui.base.list.HubListFragment
import net.xzos.upgradeall.ui.base.recycleview.RecyclerViewHolder


fun getAppHubListFragment(index: Int, appType: String): Fragment {
    return when (index) {
        0 -> UpdateAppHubListFragment(appType)
        1 -> StarAppHubListFragment(appType)
        else -> NormalAppHubListFragment(appType)
    }
}

abstract class AppHubListFragment<L : BaseAppListItemView, LV : RecyclerViewHolder<L>>(private val sAppType: String) : HubListFragment<L, LV>() {

    abstract override val viewModel: AppHubListViewModel<L>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        viewModel.mAppType = sAppType
        return super.onCreateView(inflater, container, savedInstanceState)
    }
}