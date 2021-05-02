package net.xzos.upgradeall.ui.filemanagement

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import net.xzos.upgradeall.databinding.ItemHubFileTaskerBinding
import net.xzos.upgradeall.ui.base.recycleview.RecyclerViewAdapter

class FileHubListAdapter(context: Context) : RecyclerViewAdapter<FileItemView, FileItemView, FileHubListItemHandler, FileHubListViewHolder>() {

    override val handler = FileHubListItemHandler(context)

    override fun getViewHolder(layoutInflater: LayoutInflater, viewGroup: ViewGroup): FileHubListViewHolder {
        val binding = ItemHubFileTaskerBinding.inflate(layoutInflater, viewGroup, false)
        return FileHubListViewHolder(binding)
    }
}