package net.xzos.upgradeall.ui.base.list

import android.graphics.Color
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import net.xzos.upgradeall.R
import net.xzos.upgradeall.ui.base.recycleview.ListContainerViewModel
import net.xzos.upgradeall.ui.base.recycleview.RecyclerViewAdapter
import net.xzos.upgradeall.ui.base.recycleview.RecyclerViewHolder

interface HubListPart<T, L : ListItemView, out RH : RecyclerViewHolder<in L, *, *>> {

    val rvList: RecyclerView
    val srlContainer: SwipeRefreshLayout?
    val adapter: RecyclerViewAdapter<in T, in L, *, out RH>
    val viewModel: ListContainerViewModel<T>

    fun initViewData(lifecycleOwner: LifecycleOwner) {
        adapter.lifecycleScope = lifecycleOwner.lifecycleScope
        rvList.apply {
            adapter = this@HubListPart.adapter
        }
        viewModel.getLiveData().observe(lifecycleOwner) { triple ->
            adapter.setAdapterData(triple.first, triple.second, triple.third)
            srlContainer?.isRefreshing = false
        }
        srlContainer?.apply {
            setProgressBackgroundColorSchemeResource(R.color.colorPrimary)
            setColorSchemeColors(Color.WHITE)
            setOnRefreshListener {
                refreshList()
            }
        }
        refreshList()
    }

    fun refreshList() {
        srlContainer?.isRefreshing = true
        viewModel.loadData()
    }
}