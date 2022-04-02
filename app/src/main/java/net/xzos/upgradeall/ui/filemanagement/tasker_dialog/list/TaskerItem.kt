package net.xzos.upgradeall.ui.filemanagement.tasker_dialog.list

import net.xzos.upgradeall.core.downloader.filedownloader.item.TaskWrapper
import net.xzos.upgradeall.ui.base.list.ListItemView
import net.xzos.upgradeall.utils.progress

class TaskerItem(
    val name: String,
    val path: String,
    val progress: Int,
) : ListItemView

fun TaskWrapper.getTaskerItem(): TaskerItem {
    return TaskerItem(file.name, file.path, snap.progress().toInt())
}