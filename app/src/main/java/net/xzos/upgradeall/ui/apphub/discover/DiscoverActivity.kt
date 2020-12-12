package net.xzos.upgradeall.ui.apphub.discover

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
        viewModel.cloudApplications.observe(this, {
            adapter.setList(it)
        })

        viewModel.requestCloudApplications()
    }
}