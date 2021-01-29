package net.xzos.upgradeall.ui.apphub.apps

import android.os.Bundle
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import net.xzos.upgradeall.ui.apphub.HubListFragment
import net.xzos.upgradeall.ui.apphub.adapter.AppHubListAdapter
import net.xzos.upgradeall.ui.viewmodels.view.AppListItemView
import net.xzos.upgradeall.ui.viewmodels.viewmodel.AppHubListViewModel

private const val EXTRA_INDEX = "EXTRA_INDEX"

class AppHubListFragment : HubListFragment<AppListItemView>() {

    override val adapter = AppHubListAdapter()
    override val viewModel by viewModels<AppHubListViewModel>()

    companion object {
        fun newInstance(index: Int): AppHubListFragment {
            return AppHubListFragment().apply {
                arguments = Bundle().apply {
                    putInt(EXTRA_INDEX, index)
                }
            }
        }
    }
}