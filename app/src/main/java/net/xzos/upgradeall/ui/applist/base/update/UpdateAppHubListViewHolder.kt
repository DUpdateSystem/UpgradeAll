package net.xzos.upgradeall.ui.applist.base.update

import net.xzos.upgradeall.databinding.ItemHubAppUpdateBinding
import net.xzos.upgradeall.ui.applist.base.AppHubListViewHolder

class UpdateAppHubListViewHolder(private val binding: ItemHubAppUpdateBinding, private val handler: UpdateAppHubListItemHandler)
    : AppHubListViewHolder<UpdateAppListItemView, UpdateAppHubListItemHandler>(binding.mainInfo, binding) {

    override fun doBind(itemView: UpdateAppListItemView) {
        super.doBind(itemView)
        binding.item = itemView
        binding.handler = handler
    }
}