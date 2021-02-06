package net.xzos.upgradeall.ui.base.list

import android.graphics.Color
import androidx.core.app.ComponentActivity
import androidx.lifecycle.LifecycleOwner
import com.absinthe.libraries.utils.extensions.addPaddingBottom
import com.absinthe.libraries.utils.utils.UiUtils
import net.xzos.upgradeall.R
import net.xzos.upgradeall.databinding.FragmentHubListBinding
import net.xzos.upgradeall.ui.base.recycleview.ListContainerViewModel
import net.xzos.upgradeall.ui.base.recycleview.RecyclerViewAdapter
import net.xzos.upgradeall.ui.base.recycleview.RecyclerViewHolder

interface HubListPart<L : ListItemView, out T : RecyclerViewHolder<in L>> {

    var binding: FragmentHubListBinding
    val adapter: RecyclerViewAdapter<in L, out T>
    val viewModel: ListContainerViewModel<L>

    fun initView(activity: ComponentActivity, lifecycleOwner: LifecycleOwner) {
        binding.rvList.apply {
            adapter = this@HubListPart.adapter
            addPaddingBottom(UiUtils.getNavBarHeight(activity.windowManager))
        }
        viewModel.getList().observe(lifecycleOwner) {
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

    private fun refreshList() {
        binding.srlContainer.isRefreshing = true
        viewModel.loadData()
    }
}