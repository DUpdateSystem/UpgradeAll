package net.xzos.upgradeall.ui.apphub.apps

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import net.xzos.upgradeall.ui.apphub.HubListFragment
import net.xzos.upgradeall.ui.apphub.adapter.AppHubListAdapter
import net.xzos.upgradeall.ui.viewmodels.view.AppListItemView
import net.xzos.upgradeall.ui.viewmodels.viewmodel.AppHubListViewModel


private const val EXTRA_INDEX = "EXTRA_INDEX"
private const val EXTRA_APP_TYPE = "EXTRA_APP_TYPE"

class AppHubListFragment : HubListFragment<AppListItemView>() {

    override val adapter = AppHubListAdapter()
    override val viewModel by viewModels<AppHubListViewModel>()
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val appType = arguments?.getString(EXTRA_APP_TYPE)!!
        val index = arguments?.getInt(EXTRA_INDEX)!!
        viewModel.setAppType(appType)
        viewModel.setTabPageIndex(index)
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    companion object {
        fun newInstance(index: Int, appType: String): AppHubListFragment {
            return AppHubListFragment().apply {
                arguments = Bundle().apply {
                    putInt(EXTRA_INDEX, index)
                    putString(EXTRA_APP_TYPE, appType)
                }
            }
        }
    }
}