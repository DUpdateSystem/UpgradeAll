package net.xzos.upgradeall.ui.applist.base.normal

import android.view.View
import net.xzos.upgradeall.databinding.ItemHubAppNormalBinding
import net.xzos.upgradeall.ui.base.recycleview.RecyclerViewHolder

class NormalAppHubListViewHolder(private val binding: ItemHubAppNormalBinding) : RecyclerViewHolder<NormalAppListItemView>(binding) {

    override fun doBind(itemView: NormalAppListItemView) {
        binding.mainInfo.appItem = itemView
        binding.item = itemView
    }

    override suspend fun loadExtraUi(itemView: NormalAppListItemView) {
        binding.ivStatus.setImageResource(itemView.getStatusIcon())
        binding.ivStatus.visibility = View.VISIBLE
        binding.pbStatus.visibility = View.GONE
    }
}