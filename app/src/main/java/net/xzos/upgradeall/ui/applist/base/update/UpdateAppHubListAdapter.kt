package net.xzos.upgradeall.ui.applist.base.update

import android.view.LayoutInflater
import android.view.ViewGroup
import net.xzos.upgradeall.databinding.ItemHubAppUpdateBinding
import net.xzos.upgradeall.ui.base.recycleview.RecyclerViewAdapter

class UpdateAppHubListAdapter : RecyclerViewAdapter<UpdateAppListItemView, UpdateAppHubListViewHolder>() {

    override fun getViewHolder(layoutInflater: LayoutInflater, viewGroup: ViewGroup): UpdateAppHubListViewHolder {
        val binding = ItemHubAppUpdateBinding.inflate(layoutInflater, viewGroup, false)
        return UpdateAppHubListViewHolder(binding)
    }
}