package net.xzos.upgradeall.ui.filemanagement

import android.content.Context
import net.xzos.upgradeall.core.filetasker.FileTasker
import net.xzos.upgradeall.ui.base.recycleview.RecyclerViewHandler
import net.xzos.upgradeall.ui.filemanagement.tasker_dialog.TaskerListDialog

class FileHubListItemHandler(private val context: Context) : RecyclerViewHandler() {
    fun showDialog(fileTasker: FileTasker) {
        TaskerListDialog(context, fileTasker).show()
    }
}