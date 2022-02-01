package net.xzos.upgradeall.ui.base.selectlistdialog

import net.xzos.upgradeall.databinding.ItemEnableListBinding
import net.xzos.upgradeall.ui.base.recycleview.RecyclerViewHolder

class SelectItemHolder(val binding: ItemEnableListBinding)
    : RecyclerViewHolder<SelectItem, SelectItemHandler, ItemEnableListBinding>(binding, binding) {
    override fun doBind(itemView: SelectItem) {
        binding.item = itemView
    }

    override fun setHandler(handler: SelectItemHandler) {
        binding.handler = handler
    }
}