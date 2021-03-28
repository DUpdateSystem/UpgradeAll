package net.xzos.upgradeall.ui.applist.base.normal

import net.xzos.upgradeall.databinding.ItemHubAppNormalBinding
import net.xzos.upgradeall.ui.applist.base.AppHubListViewHolder

class NormalAppHubListViewHolder(private val binding: ItemHubAppNormalBinding)
    : AppHubListViewHolder<NormalAppListItemView, NormalAppHubListItemHandler>(binding.mainInfo, binding) {

    override fun doBind(itemView: NormalAppListItemView) {
        super.doBind(itemView)
        binding.item = itemView
    }
}