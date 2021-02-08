package net.xzos.upgradeall.ui.applist.base

import androidx.databinding.ViewDataBinding
import net.xzos.upgradeall.databinding.ItemHubAppBinding
import net.xzos.upgradeall.ui.base.recycleview.RecyclerViewHolder

abstract class AppHubListViewHolder<L : BaseAppListItemView, H : AppHubListItemHandler>(listBinding: ItemHubAppBinding, binding: ViewDataBinding)
    : RecyclerViewHolder<L, H, ItemHubAppBinding>(listBinding, binding) {

    override fun doBind(itemView: L) {
        listBinding.appItem = itemView
    }

    override fun setHandler(handler: H) {
        listBinding.handler = handler
    }
}