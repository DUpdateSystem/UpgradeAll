package net.xzos.upgradeall.ui.filemanagement

import net.xzos.upgradeall.databinding.ItemHubFileTaskerBinding
import net.xzos.upgradeall.ui.base.recycleview.RecyclerViewHolder


class FileHubListViewHolder(private val binding: ItemHubFileTaskerBinding)
    : RecyclerViewHolder<FileItemView, FileHubListItemHandler, ItemHubFileTaskerBinding>(binding, binding) {

    override fun doBind(itemView: FileItemView) {
        binding.fileItem = itemView
    }

    override fun setHandler(handler: FileHubListItemHandler) {
        binding.hander = handler
    }
}
