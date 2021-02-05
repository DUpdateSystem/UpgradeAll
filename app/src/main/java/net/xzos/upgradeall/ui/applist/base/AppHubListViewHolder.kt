package net.xzos.upgradeall.ui.applist.base

import net.xzos.upgradeall.databinding.ItemHubAppBinding
import net.xzos.upgradeall.ui.base.recycleview.RecyclerViewHolder

class AppHubListViewHolder(private val binding: ItemHubAppBinding) : RecyclerViewHolder<AppListItemView>(binding) {

    override fun doBind(itemView: AppListItemView) {
        binding.appItem = itemView
    }
}
