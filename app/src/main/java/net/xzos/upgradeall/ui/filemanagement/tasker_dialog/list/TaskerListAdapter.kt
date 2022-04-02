package net.xzos.upgradeall.ui.filemanagement.tasker_dialog.list

import android.view.LayoutInflater
import android.view.ViewGroup
import net.xzos.upgradeall.core.downloader.filedownloader.item.TaskWrapper
import net.xzos.upgradeall.databinding.ItemFileTaskerBinding
import net.xzos.upgradeall.ui.base.recycleview.RecyclerViewAdapter

class TaskerListAdapter :
    RecyclerViewAdapter<TaskWrapper, TaskerItem, TaskerItemHandler, TaskerItemHolder>({ it.getTaskerItem() }) {
    override val handler = TaskerItemHandler()

    override fun getViewHolder(
        layoutInflater: LayoutInflater,
        viewGroup: ViewGroup
    ): TaskerItemHolder {
        return TaskerItemHolder(ItemFileTaskerBinding.inflate(layoutInflater, viewGroup, false))
    }
}