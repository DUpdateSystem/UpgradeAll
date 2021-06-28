package net.xzos.upgradeall.ui.base.list

import android.graphics.Color
import androidx.core.app.ComponentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import net.xzos.upgradeall.R
import net.xzos.upgradeall.databinding.RecyclerlistContentBinding
import net.xzos.upgradeall.ui.base.recycleview.ListContainerViewModel
import net.xzos.upgradeall.ui.base.recycleview.RecyclerViewAdapter
import net.xzos.upgradeall.ui.base.recycleview.RecyclerViewHolder

interface HubListPart<T, L : ListItemView, out RH : RecyclerViewHolder<in L, *, *>> {

    val binding: RecyclerlistContentBinding
    val adapter: RecyclerViewAdapter<in T, in L, *, out RH>
    val viewModel: ListContainerViewModel<T>

    fun initView(activity: ComponentActivity, lifecycleOwner: LifecycleOwner) {
        adapter.lifecycleScope = activity.lifecycleScope
        binding.rvList.apply {
            adapter = this@HubListPart.adapter
        }
        viewModel.getLiveData().observe(lifecycleOwner) { triple ->
            adapter.setAdapterData(triple.first, triple.second, triple.third)
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

    fun refreshList() {
        binding.srlContainer.isRefreshing = true
        viewModel.loadData()
    }
}