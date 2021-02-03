package net.xzos.upgradeall.ui.filemanagement

import net.xzos.upgradeall.databinding.ItemHubFileBinding
import net.xzos.upgradeall.ui.base.recycleview.RecyclerViewHolder


class FileHubListViewHolder(private val binding: ItemHubFileBinding) : RecyclerViewHolder<FileItemView>(binding) {

    override fun doBind(itemView: FileItemView) {
        binding.fileItem = itemView
    }
}
