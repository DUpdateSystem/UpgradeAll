package net.xzos.upgradeall.ui.applist.base.normal

import net.xzos.upgradeall.databinding.ItemHubAppNormalBinding
import net.xzos.upgradeall.ui.base.recycleview.RecyclerViewHolder

class NormalAppHubListViewHolder(private val binding: ItemHubAppNormalBinding) : RecyclerViewHolder<NormalAppListItemView>(binding) {

    override fun doBind(itemView: NormalAppListItemView) {
        binding.mainInfo.appItem = itemView
        binding.item = itemView
    }
}