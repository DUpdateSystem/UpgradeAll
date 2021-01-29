package net.xzos.upgradeall.ui.apphub.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import net.xzos.upgradeall.databinding.ItemHubFileBinding
import net.xzos.upgradeall.ui.viewmodels.view.FileItemView
import net.xzos.upgradeall.ui.viewmodels.view.RecyclerViewAdapter
import net.xzos.upgradeall.ui.viewmodels.view.holder.FileHubListViewHolder

class FileHubListAdapter : RecyclerViewAdapter<FileItemView, FileHubListViewHolder>() {

    override fun getViewHolder(layoutInflater: LayoutInflater, viewGroup: ViewGroup): FileHubListViewHolder {
        val binding = ItemHubFileBinding.inflate(layoutInflater, viewGroup, false)
        return FileHubListViewHolder(binding)
    }
}