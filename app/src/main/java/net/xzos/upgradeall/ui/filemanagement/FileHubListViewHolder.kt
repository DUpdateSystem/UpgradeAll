package net.xzos.upgradeall.ui.filemanagement

import net.xzos.upgradeall.databinding.ItemHubFileBinding
import net.xzos.upgradeall.ui.base.recycleview.RecyclerViewHolder


class FileHubListViewHolder(private val binding: ItemHubFileBinding)
    : RecyclerViewHolder<FileItemView, FileHubListItemHandler, ItemHubFileBinding>(binding, binding) {

    override fun doBind(itemView: FileItemView) {
        binding.fileItem = itemView
    }

    override fun setHandler(handler: FileHubListItemHandler) {
        // TODO("Not yet implemented")
    }
}
