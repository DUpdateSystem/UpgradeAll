package net.xzos.upgradeall.ui.applist.base.normal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import net.xzos.upgradeall.databinding.FragmentHubListBinding
import net.xzos.upgradeall.ui.applist.base.AppHubListFragment


open class NormalAppHubListFragment
    : AppHubListFragment<NormalAppListItemView, NormalAppHubListViewHolder>() {
    override val adapter = NormalAppHubListAdapter(
        listContainerViewConvertFun = {
            NormalAppListItemView(it).apply { renew(requireContext()) }
        })

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return FragmentHubListBinding.inflate(inflater).also {
            initView(it.listLayout.rvList, it.listLayout.srlContainer)
        }.root
    }
}