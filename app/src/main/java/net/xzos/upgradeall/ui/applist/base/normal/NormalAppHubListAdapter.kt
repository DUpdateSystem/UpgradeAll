package net.xzos.upgradeall.ui.applist.base.normal

import android.view.LayoutInflater
import android.view.ViewGroup
import net.xzos.upgradeall.databinding.ItemHubAppNormalBinding
import net.xzos.upgradeall.ui.base.recycleview.RecyclerViewAdapter

class NormalAppHubListAdapter : RecyclerViewAdapter<NormalAppListItemView, NormalAppHubListViewHolder>() {

    override fun getViewHolder(layoutInflater: LayoutInflater, viewGroup: ViewGroup): NormalAppHubListViewHolder {
        val binding = ItemHubAppNormalBinding.inflate(layoutInflater, viewGroup, false)
        return NormalAppHubListViewHolder(binding)
    }
}