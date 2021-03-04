package net.xzos.upgradeall.ui.filemanagement.tasker_dialog

import net.xzos.upgradeall.databinding.ItemFileTaskerBinding
import net.xzos.upgradeall.ui.base.listdialog.DialogListAdapter

class TaskerListAdapter(dataList: List<TaskerItem>)
    : DialogListAdapter<TaskerItem, TaskerItemHandler, TaskerItemHolder>(dataList, TaskerItemHandler(),
        fun(layoutInflater, viewGroup) = TaskerItemHolder(ItemFileTaskerBinding.inflate(layoutInflater, viewGroup, false))
)