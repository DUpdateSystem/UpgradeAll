package net.xzos.upgradeall.ui.apphub

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import net.xzos.upgradeall.databinding.FragmentHubListBinding
import net.xzos.upgradeall.ui.apphub.adapter.HubListAdapter
import net.xzos.upgradeall.ui.viewmodels.view.ItemCardView
import net.xzos.upgradeall.ui.viewmodels.view.ItemCardViewExtraData

class HubListFragment : Fragment() {

    private lateinit var binding: FragmentHubListBinding
    private val adapter = HubListAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentHubListBinding.inflate(inflater)
        initView()
        return binding.root
    }

    private fun initView() {
        binding.apply {
            rvList.apply {
                adapter = this@HubListFragment.adapter
            }
        }
        val list = mutableListOf<ItemCardView>()
        var itemCardView: ItemCardView
        for (i in 1..10) {
            itemCardView = ItemCardView(name = i.toString(), extraData = ItemCardViewExtraData(uuid = "123456789"))
            list.add(itemCardView)
        }
        adapter.setList(list)
    }
}