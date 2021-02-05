package net.xzos.upgradeall.ui.applist.base.update

import net.xzos.upgradeall.databinding.ItemHubAppUpdateBinding
import net.xzos.upgradeall.ui.base.recycleview.RecyclerViewHolder

class UpdateAppHubListViewHolder(private val binding: ItemHubAppUpdateBinding)
    : RecyclerViewHolder<UpdateAppListItemView>(binding) {

    override fun doBind(itemView: UpdateAppListItemView) {
        binding.mainInfo.appItem = itemView
        binding.item = itemView
    }
}