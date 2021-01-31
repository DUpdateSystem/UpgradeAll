package net.xzos.upgradeall.ui.apphub

import android.graphics.Color
import android.view.View
import androidx.appcompat.widget.Toolbar
import net.xzos.upgradeall.R
import net.xzos.upgradeall.databinding.ActivityDiscoverBinding
import net.xzos.upgradeall.ui.base.AppBarActivity
import net.xzos.upgradeall.ui.viewmodels.view.ListItemView
import net.xzos.upgradeall.ui.viewmodels.view.RecyclerViewAdapter
import net.xzos.upgradeall.ui.viewmodels.view.holder.RecyclerViewHolder
import net.xzos.upgradeall.ui.viewmodels.viewmodel.ListContainerViewModel

abstract class HubListActivity<L : ListItemView, T : RecyclerViewHolder<L>> : AppBarActivity() {
    protected lateinit var binding: ActivityDiscoverBinding
    protected abstract val adapter: RecyclerViewAdapter<L, T>
    protected abstract val viewModel: ListContainerViewModel<L>

    override fun initView() {
        binding.rvList.apply { adapter = this@HubListActivity.adapter }
        viewModel.getList().observe(this) {
            adapter.dataSet = it
            binding.srlContainer.isRefreshing = false
        }
        binding.srlContainer.apply {
            setProgressBackgroundColorSchemeResource(R.color.colorPrimary)
            setColorSchemeColors(Color.WHITE)
            setOnRefreshListener {
                refreshList()
            }
        }
        refreshList()
    }

    override fun getAppBar(): Toolbar = binding.appbar.toolbar

    override fun initBinding(): View {
        binding = ActivityDiscoverBinding.inflate(layoutInflater)
        return binding.root
    }

    private fun refreshList() {
        binding.srlContainer.isRefreshing = true
        viewModel.loadData()
    }
}