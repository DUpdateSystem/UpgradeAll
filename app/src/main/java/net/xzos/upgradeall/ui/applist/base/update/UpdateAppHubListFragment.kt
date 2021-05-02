package net.xzos.upgradeall.ui.applist.base.update

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import net.xzos.upgradeall.R
import net.xzos.upgradeall.databinding.FragmentHubUpdateListBinding
import net.xzos.upgradeall.ui.applist.base.AppHubListFragment


class UpdateAppHubListFragment : AppHubListFragment<UpdateAppListItemView, UpdateAppHubListViewHolder>() {

    lateinit var rootBinding: FragmentHubUpdateListBinding
    override val adapter = UpdateAppHubListAdapter(
            listContainerViewConvertFun = {
                UpdateAppListItemView(it).apply { renew(requireContext()) }
            })

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        rootBinding = FragmentHubUpdateListBinding.inflate(inflater)
        initView(rootBinding.fragmentHubList)

        viewModel.getLiveData().observe(viewLifecycleOwner, { triple ->
            rootBinding.tvAppUpdateTip.text = String.format(getString(R.string.hub_format_app_update_tip), triple.first.size)
        })
        return binding.root
    }
}