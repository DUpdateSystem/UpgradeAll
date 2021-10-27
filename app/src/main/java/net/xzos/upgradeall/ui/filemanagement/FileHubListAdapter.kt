package net.xzos.upgradeall.ui.filemanagement

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import net.xzos.upgradeall.databinding.ItemHubFileTaskerBinding
import net.xzos.upgradeall.ui.base.recycleview.RecyclerViewAdapter

class FileHubListAdapter(activity: AppCompatActivity) :
    RecyclerViewAdapter<FileItemView, FileItemView, FileHubListItemHandler, FileHubListViewHolder>() {

    override val handler = FileHubListItemHandler(activity)

    override fun getViewHolder(
        layoutInflater: LayoutInflater,
        viewGroup: ViewGroup
    ): FileHubListViewHolder {
        val binding = ItemHubFileTaskerBinding.inflate(layoutInflater, viewGroup, false)
        return FileHubListViewHolder(binding)
    }
}