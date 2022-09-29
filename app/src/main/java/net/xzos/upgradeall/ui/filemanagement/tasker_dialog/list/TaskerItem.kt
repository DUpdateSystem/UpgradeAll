package net.xzos.upgradeall.ui.filemanagement.tasker_dialog.list

import net.xzos.upgradeall.core.downloader.filedownloader.item.TaskWrapper
import net.xzos.upgradeall.core.downloader.filedownloader.item.progress
import net.xzos.upgradeall.ui.base.list.ListItemView

class TaskerItem(
    val name: String,
    val path: String,
    var progress: Int,
) : ListItemView

fun TaskWrapper.getTaskerItem(): TaskerItem {
    return TaskerItem(file.name, file.path, snap.progress().toInt()).also { item ->
        this@getTaskerItem.observe {
            item.progress = it.progress().toInt()
        }
    }
}