package net.xzos.upgradeall.ui.apphub

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import net.xzos.upgradeall.databinding.FragmentHubListBinding
import net.xzos.upgradeall.ui.apphub.adapter.HubListAdapter
import net.xzos.upgradeall.ui.detail.AppDetailActivity
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
        adapter.setOnItemClickListener { _, _, position ->
            AppDetailActivity.startActivity(requireContext(), adapter.data[position].app)
        }

        viewModel.appCardViewList.observe(viewLifecycleOwner, {
            adapter.setList(it)
        })
    }

    override fun onResume() {
        super.onResume()
        viewModel.itemCountLiveData.value = adapter.itemCount
    }
}
