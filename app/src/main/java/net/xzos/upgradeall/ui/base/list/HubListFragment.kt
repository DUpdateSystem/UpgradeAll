package net.xzos.upgradeall.ui.base.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import net.xzos.upgradeall.databinding.FragmentHubListBinding
import net.xzos.upgradeall.ui.base.recycleview.RecyclerViewHolder

abstract class HubListFragment<T, L : ListItemView, RH : RecyclerViewHolder<L>> : HubListPart<T, L, RH>, Fragment() {

    override lateinit var binding: FragmentHubListBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return FragmentHubListBinding.inflate(inflater).also {
            initView(it)
        }.root
    }

    fun initView(binding: FragmentHubListBinding) {
        this.binding = binding
        val activity = activity ?: throw RuntimeException("No Activity")
        initView(activity, viewLifecycleOwner)
    }
}