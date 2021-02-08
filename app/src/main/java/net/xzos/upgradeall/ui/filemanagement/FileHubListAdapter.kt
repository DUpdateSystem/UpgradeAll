package net.xzos.upgradeall.ui.filemanagement

import android.view.LayoutInflater
import android.view.ViewGroup
import net.xzos.upgradeall.databinding.ItemHubFileBinding
import net.xzos.upgradeall.ui.base.recycleview.RecyclerViewAdapter

class FileHubListAdapter : RecyclerViewAdapter<FileItemView, FileHubListItemHandler, FileHubListViewHolder>() {

    override val handler: FileHubListItemHandler?
        get() = TODO("Not yet implemented")

    override fun getViewHolder(layoutInflater: LayoutInflater, viewGroup: ViewGroup): FileHubListViewHolder {
        val binding = ItemHubFileBinding.inflate(layoutInflater, viewGroup, false)
        return FileHubListViewHolder(binding)
    }
}