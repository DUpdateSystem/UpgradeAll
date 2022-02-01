package net.xzos.upgradeall.ui.detail.download

import net.xzos.upgradeall.databinding.ItemDownloadDialogBinding
import net.xzos.upgradeall.ui.base.recycleview.RecyclerViewHolder

class DownloadHolder(private val binding: ItemDownloadDialogBinding)
    : RecyclerViewHolder<DownloadItem, DownloadItemHandler, ItemDownloadDialogBinding>(binding, binding) {
    override fun doBind(itemView: DownloadItem) {
        binding.item = itemView
    }

    override fun setHandler(handler: DownloadItemHandler) {
        binding.handler = handler
    }
}