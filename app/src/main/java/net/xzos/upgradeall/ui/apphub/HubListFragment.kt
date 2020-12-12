package net.xzos.upgradeall.ui.apphub

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.xzos.upgradeall.databinding.FragmentHubListBinding
import net.xzos.upgradeall.ui.apphub.adapter.HubListAdapter
import net.xzos.upgradeall.ui.viewmodels.view.ItemCardView
import net.xzos.upgradeall.ui.viewmodels.view.ItemCardViewExtraData
import net.xzos.upgradeall.ui.viewmodels.viewmodel.AppHubViewModel

class HubListFragment : Fragment() {

    private lateinit var binding: FragmentHubListBinding
    private val adapter = HubListAdapter()
    private val viewModel by activityViewModels<AppHubViewModel>()

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
        adapter.setOnItemClickListener { adapter, view, position ->

        }

        lifecycleScope.launch(Dispatchers.IO) {
            val list = mutableListOf<ItemCardView>()
            var itemCardView: ItemCardView
            for (i in 1..(1..10).random()) {
                itemCardView = ItemCardView(name = i.toString(), extraData = ItemCardViewExtraData(uuid = "123456789"))
                list.add(itemCardView)
            }
            withContext(Dispatchers.Main) {
                adapter.setList(list)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.itemCountLiveData.value = adapter.itemCount
    }
}