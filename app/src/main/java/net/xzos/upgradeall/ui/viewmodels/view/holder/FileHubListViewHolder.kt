package net.xzos.upgradeall.ui.viewmodels.view.holder

import net.xzos.upgradeall.databinding.ItemHubFileBinding
import net.xzos.upgradeall.ui.viewmodels.view.FileItemView


class FileHubListViewHolder(private val binding: ItemHubFileBinding) : RecyclerViewHolder<FileItemView>(binding) {

    override fun doBind(itemView: FileItemView) {
        binding.fileItem = itemView
    }
}
