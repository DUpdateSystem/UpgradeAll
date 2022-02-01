package net.xzos.upgradeall.ui.filemanagement.tasker_dialog.list

import com.tonyodev.fetch2.Download
import net.xzos.upgradeall.ui.base.list.ListItemView
import java.io.File

class TaskerItem(
    val name: String,
    val path: String,
    val progress: Int,
) : ListItemView

fun Download.getTaskerItem(): TaskerItem {
    val file = File(file)
    return TaskerItem(file.name, file.path, progress)
}