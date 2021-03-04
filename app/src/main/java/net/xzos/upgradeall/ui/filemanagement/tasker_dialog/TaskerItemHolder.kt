package net.xzos.upgradeall.ui.filemanagement.tasker_dialog

import net.xzos.upgradeall.databinding.ItemFileTaskerBinding
import net.xzos.upgradeall.ui.base.recycleview.RecyclerViewHolder

class TaskerItemHolder(private val binding: ItemFileTaskerBinding)
    : RecyclerViewHolder<TaskerItem, TaskerItemHandler, ItemFileTaskerBinding>(binding, binding) {
    override fun doBind(itemView: TaskerItem) {
        binding.fileItem = itemView
    }

    override fun setHandler(handler: TaskerItemHandler) {
        binding.handler = handler
    }
}