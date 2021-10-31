package net.xzos.upgradeall.ui.filemanagement.tasker_dialog.list

import net.xzos.upgradeall.databinding.ItemFileTaskerBinding
import net.xzos.upgradeall.ui.base.listdialog.DialogListAdapter

class TaskerListAdapter(dataList: List<TaskerItem>) :
    DialogListAdapter<TaskerItem, TaskerItemHandler, TaskerItemHolder>(
        dataList.toMutableList(), TaskerItemHandler(),
        fun(layoutInflater, viewGroup) =
            TaskerItemHolder(ItemFileTaskerBinding.inflate(layoutInflater, viewGroup, false))
    )