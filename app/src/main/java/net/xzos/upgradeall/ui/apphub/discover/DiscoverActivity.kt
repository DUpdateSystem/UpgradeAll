package net.xzos.upgradeall.ui.apphub.discover

import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.widget.Toolbar
import net.xzos.upgradeall.databinding.ActivityDiscoverBinding
import net.xzos.upgradeall.ui.apphub.adapter.DiscoveryAdapter
import net.xzos.upgradeall.ui.base.AppBarActivity
import net.xzos.upgradeall.ui.viewmodels.viewmodel.DiscoveryViewModel


class DiscoverActivity : AppBarActivity() {

    private lateinit var binding: ActivityDiscoverBinding
    private val viewModel by viewModels<DiscoveryViewModel>()
    private val adapter = DiscoveryAdapter()

    override fun initBinding(): View {
        binding = ActivityDiscoverBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun getAppBar(): Toolbar = binding.appbar.toolbar

    override fun initView() {
        binding.rvList.apply {
            adapter = this@DiscoverActivity.adapter
        }
        binding.srlContainer.setOnRefreshListener {
            binding.srlContainer.isRefreshing = true
            viewModel.requestCloudApplications()
        }
        adapter.setOnItemClickListener { _, _, position ->
            adapter.getItem(position).uuid?.let {
                viewModel.downloadApplicationData(this, it)
            } ?: let {
                Log.d("DiscoverActivity", "uuid is null")
            }
        }
        viewModel.cloudApplications.observe(this, {
            adapter.setList(it)
            binding.srlContainer.isRefreshing = false
        })

        viewModel.requestCloudApplications()
    }
}