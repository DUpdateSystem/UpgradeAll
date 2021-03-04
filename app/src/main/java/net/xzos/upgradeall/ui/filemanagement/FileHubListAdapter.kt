package net.xzos.upgradeall.ui.filemanagement

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import net.xzos.upgradeall.databinding.ItemHubFileTaskerBinding
import net.xzos.upgradeall.ui.base.recycleview.RecyclerViewAdapter

class FileHubListAdapter(
        supportFragmentManager: FragmentManager,
) : RecyclerViewAdapter<FileItemView, FileHubListItemHandler, FileHubListViewHolder>() {

    override val handler = FileHubListItemHandler(supportFragmentManager)

    override fun getViewHolder(layoutInflater: LayoutInflater, viewGroup: ViewGroup): FileHubListViewHolder {
        val binding = ItemHubFileTaskerBinding.inflate(layoutInflater, viewGroup, false)
        return FileHubListViewHolder(binding)
    }
}