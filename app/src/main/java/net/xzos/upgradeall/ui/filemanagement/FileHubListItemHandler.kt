package net.xzos.upgradeall.ui.filemanagement

import androidx.appcompat.app.AppCompatActivity
import net.xzos.upgradeall.core.downloader.filetasker.FileTasker
import net.xzos.upgradeall.ui.base.recycleview.RecyclerViewHandler
import net.xzos.upgradeall.ui.filemanagement.tasker_dialog.TaskerListDialog

class FileHubListItemHandler(private val activity: AppCompatActivity) : RecyclerViewHandler() {
    fun showDialog(fileTasker: FileTasker) {
        TaskerListDialog.newInstance(activity, fileTasker)
    }
}