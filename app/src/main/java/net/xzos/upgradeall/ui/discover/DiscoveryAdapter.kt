package net.xzos.upgradeall.ui.discover

import android.view.LayoutInflater
import android.view.ViewGroup
import net.xzos.upgradeall.databinding.ItemDiscoverAppBinding
import net.xzos.upgradeall.ui.base.recycleview.RecyclerViewAdapter

class DiscoveryAdapter(
        override val handler: DiscoverListItemHandler
) : RecyclerViewAdapter<DiscoverListItemView, DiscoverListItemView, DiscoverListItemHandler, DiscoverListViewHolder>() {

    override fun getViewHolder(layoutInflater: LayoutInflater, viewGroup: ViewGroup): DiscoverListViewHolder {
        val binding = ItemDiscoverAppBinding.inflate(layoutInflater, viewGroup, false)
        return DiscoverListViewHolder(binding)
    }

    override fun getItemId(position: Int) = getAdapterData()[position].hashCode().toLong()
}