package net.xzos.upgradeall.ui.applist.base.normal

import android.view.View
import net.xzos.upgradeall.databinding.ItemHubAppNormalBinding
import net.xzos.upgradeall.ui.applist.base.AppHubListViewHolder

class NormalAppHubListViewHolder(private val binding: ItemHubAppNormalBinding)
    : AppHubListViewHolder<NormalAppListItemView, NormalAppHubListItemHandler>(binding.mainInfo, binding) {

    override fun doBind(itemView: NormalAppListItemView) {
        super.doBind(itemView)
        binding.item = itemView
    }

    override suspend fun loadExtraUi(itemView: NormalAppListItemView) {
        binding.ivStatus.setImageResource(itemView.getStatusIcon())
        binding.ivStatus.visibility = View.VISIBLE
        binding.pbStatus.visibility = View.GONE
    }
}