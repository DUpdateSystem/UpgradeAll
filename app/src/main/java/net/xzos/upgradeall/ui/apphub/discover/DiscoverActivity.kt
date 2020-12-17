package net.xzos.upgradeall.ui.apphub.discover

import android.graphics.Color
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.xzos.upgradeall.R
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
        binding.srlContainer.apply {
            setProgressBackgroundColorSchemeResource(R.color.colorPrimary)
            setColorSchemeColors(Color.WHITE)
            setOnRefreshListener {
                requestCloudApplications()
            }
        }
        adapter.apply {
            setOnItemClickListener { _, _, position ->
                getItem(position).uuid?.let {
                    viewModel.downloadApplicationData(this@DiscoverActivity, it)

                    lifecycleScope.launch(Dispatchers.IO) {
                        delay(500)
                        withContext(Dispatchers.Main) {
                            viewModel.requestCloudApplications()
                        }
                    }
                } ?: let {
                    Log.d("DiscoverActivity", "uuid is null")
                }
            }
        }
        viewModel.cloudApplications.observe(this, {
            adapter.setList(it)
            binding.srlContainer.isRefreshing = false
        })

        requestCloudApplications()
    }

    private fun requestCloudApplications() {
        binding.srlContainer.isRefreshing = true
        viewModel.requestCloudApplications()
    }
}