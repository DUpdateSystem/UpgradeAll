package net.xzos.upgradeall.ui.hubmanager

import net.xzos.upgradeall.databinding.ItemHubBinding
import net.xzos.upgradeall.ui.base.recycleview.RecyclerViewHolder

class HubManagerListViewHolder(private val binding: ItemHubBinding)
    : RecyclerViewHolder<HubManagerListItemView, HubManagerListItemHandler, ItemHubBinding>(binding, binding) {

    override fun setHandler(handler: HubManagerListItemHandler) {
        binding.handler = handler
    }

    override fun doBind(itemView: HubManagerListItemView) {
        binding.item = itemView
    }
}