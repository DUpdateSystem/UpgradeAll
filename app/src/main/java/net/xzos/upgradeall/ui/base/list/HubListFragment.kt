package net.xzos.upgradeall.ui.base.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import net.xzos.upgradeall.databinding.FragmentHubListBinding
import net.xzos.upgradeall.ui.applist.base.AppHubListViewModel
import net.xzos.upgradeall.ui.applist.base.BaseAppListItemView
import net.xzos.upgradeall.ui.base.recycleview.RecyclerViewHolder

abstract class HubListFragment<L : ListItemView, T : RecyclerViewHolder<L>> : HubListPart<L, T>, Fragment() {

    override lateinit var binding: FragmentHubListBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentHubListBinding.inflate(inflater)
        val activity = activity ?: throw RuntimeException("No Activity")
        initView(activity, viewLifecycleOwner)
        return binding.root
    }
}