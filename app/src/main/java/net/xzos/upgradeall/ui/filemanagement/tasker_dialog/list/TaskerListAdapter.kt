package net.xzos.upgradeall.ui.filemanagement.tasker_dialog.list

import android.view.LayoutInflater
import android.view.ViewGroup
import com.tonyodev.fetch2.Download
import net.xzos.upgradeall.databinding.ItemFileTaskerBinding
import net.xzos.upgradeall.ui.base.recycleview.RecyclerViewAdapter

class TaskerListAdapter :
    RecyclerViewAdapter<Download, TaskerItem, TaskerItemHandler, TaskerItemHolder>({ it.getTaskerItem() }) {
    override val handler = TaskerItemHandler()

    override fun getViewHolder(
        layoutInflater: LayoutInflater,
        viewGroup: ViewGroup
    ): TaskerItemHolder {
        return TaskerItemHolder(ItemFileTaskerBinding.inflate(layoutInflater, viewGroup, false))
    }
}