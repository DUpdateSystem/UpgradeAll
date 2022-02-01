package net.xzos.upgradeall.ui.hubmanager

import android.view.LayoutInflater
import android.view.ViewGroup
import net.xzos.upgradeall.databinding.ItemHubBinding
import net.xzos.upgradeall.ui.base.recycleview.RecyclerViewAdapter

class HubManagerAdapter(
        override val handler: HubManagerListItemHandler
) : RecyclerViewAdapter<HubManagerListItemView, HubManagerListItemView, HubManagerListItemHandler, HubManagerListViewHolder>() {


    override fun getViewHolder(layoutInflater: LayoutInflater, viewGroup: ViewGroup): HubManagerListViewHolder {
        val binding = ItemHubBinding.inflate(layoutInflater, viewGroup, false)
        return HubManagerListViewHolder(binding)
    }
}