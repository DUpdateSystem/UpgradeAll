package net.xzos.upgradeall.ui.base.list

import android.graphics.Color
import androidx.core.app.ComponentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.absinthe.libraries.utils.extensions.addPaddingBottom
import com.absinthe.libraries.utils.utils.UiUtils
import net.xzos.upgradeall.R
import net.xzos.upgradeall.databinding.FragmentHubListBinding
import net.xzos.upgradeall.ui.base.recycleview.ListContainerViewModel
import net.xzos.upgradeall.ui.base.recycleview.RecyclerViewAdapter
import net.xzos.upgradeall.ui.base.recycleview.RecyclerViewHolder

interface HubListPart<T, L : ListItemView, out RH : RecyclerViewHolder<in L, *, *>> {

    var binding: FragmentHubListBinding
    val adapter: RecyclerViewAdapter<in L, *, out RH>
    val viewModel: ListContainerViewModel<T>
    val listContainerViewConvertFun: (T) -> L

    fun initView(activity: ComponentActivity, lifecycleOwner: LifecycleOwner) {
        adapter.lifecycleScope = activity.lifecycleScope
        binding.rvList.apply {
            adapter = this@HubListPart.adapter
            addPaddingBottom(UiUtils.getNavBarHeight(activity.windowManager))
        }
        viewModel.getList().observe(lifecycleOwner) { list ->
            adapter.dataSet = list.map { listContainerViewConvertFun(it) }
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

    private fun refreshList() {
        binding.srlContainer.isRefreshing = true
        viewModel.loadData()
    }
}