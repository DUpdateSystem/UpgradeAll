package net.xzos.upgradeall.ui.applist.base.update

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import net.xzos.upgradeall.R
import net.xzos.upgradeall.core.module.app.App
import net.xzos.upgradeall.databinding.FragmentHubUpdateListBinding
import net.xzos.upgradeall.ui.applist.base.AppHubListFragment
import net.xzos.upgradeall.ui.applist.base.TAB_UPDATE


class UpdateAppHubListFragment()
    : AppHubListFragment<UpdateAppListItemView, UpdateAppHubListViewHolder>() {

    lateinit var rootBinding: FragmentHubUpdateListBinding
    override val adapter = UpdateAppHubListAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        rootBinding = FragmentHubUpdateListBinding.inflate(inflater)
        initView(rootBinding.fragmentHubList)

        viewModel.getList().observe(viewLifecycleOwner, {
            rootBinding.tvAppUpdateTip.text = String.format(getString(R.string.hub_format_app_update_tip), it.size)
        })
        return binding.root
    }

    override val listContainerViewConvertFun = fun(app: App) = UpdateAppListItemView(app)
}