package net.xzos.upgradeall.ui.filemanagement.tasker_dialog.list

import android.view.LayoutInflater
import android.view.ViewGroup
import net.xzos.upgradeall.databinding.ItemFileTaskerBinding
import net.xzos.upgradeall.getter.rpc.RustTaskWrapper
import net.xzos.upgradeall.ui.base.recycleview.RecyclerViewAdapter

class TaskerListAdapter :
    RecyclerViewAdapter<RustTaskWrapper, TaskerItem, TaskerItemHandler, TaskerItemHolder>({ it.getTaskerItem() }) {
    override val handler = TaskerItemHandler()

    override fun getViewHolder(
        layoutInflater: LayoutInflater,
        viewGroup: ViewGroup
    ): TaskerItemHolder {
        return TaskerItemHolder(ItemFileTaskerBinding.inflate(layoutInflater, viewGroup, false))
    }
}