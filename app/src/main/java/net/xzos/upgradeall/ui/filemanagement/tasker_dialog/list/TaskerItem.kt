package net.xzos.upgradeall.ui.filemanagement.tasker_dialog.list

import net.xzos.upgradeall.getter.rpc.RustTaskWrapper
import net.xzos.upgradeall.ui.base.list.ListItemView

class TaskerItem(
    val name: String,
    val path: String,
    var progress: Int,
) : ListItemView

fun RustTaskWrapper.getTaskerItem(): TaskerItem {
    return TaskerItem(file.name, file.path, snap.progress().toInt())
}