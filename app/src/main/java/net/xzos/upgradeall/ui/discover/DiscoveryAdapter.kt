package net.xzos.upgradeall.ui.discover

import android.view.LayoutInflater
import android.view.ViewGroup
import net.xzos.upgradeall.databinding.ItemDiscoverAppBinding
import net.xzos.upgradeall.ui.base.recycleview.RecyclerViewAdapter

class DiscoveryAdapter : RecyclerViewAdapter<DiscoverListItemView, DiscoverListViewHolder>() {

    override fun getViewHolder(layoutInflater: LayoutInflater, viewGroup: ViewGroup): DiscoverListViewHolder {
        val binding = ItemDiscoverAppBinding.inflate(layoutInflater, viewGroup, false)
        return DiscoverListViewHolder(binding)
    }
}