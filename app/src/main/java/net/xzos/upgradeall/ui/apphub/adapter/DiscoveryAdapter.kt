package net.xzos.upgradeall.ui.apphub.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import net.xzos.upgradeall.databinding.ItemDiscoverAppBinding
import net.xzos.upgradeall.ui.viewmodels.view.CloudConfigListItemView
import net.xzos.upgradeall.ui.viewmodels.view.RecyclerViewAdapter
import net.xzos.upgradeall.ui.viewmodels.view.holder.DiscoverListViewHolder

class DiscoveryAdapter : RecyclerViewAdapter<CloudConfigListItemView, DiscoverListViewHolder>() {

    override fun getViewHolder(layoutInflater: LayoutInflater, viewGroup: ViewGroup): DiscoverListViewHolder {
        val binding = ItemDiscoverAppBinding.inflate(layoutInflater, viewGroup, false)
        return DiscoverListViewHolder(binding)
    }
}