package net.xzos.upgradeall.ui.filemanagement

import androidx.appcompat.app.AppCompatActivity
import net.xzos.upgradeall.ui.base.recycleview.RecyclerViewHandler
import net.xzos.upgradeall.ui.filemanagement.tasker_dialog.TaskerListDialog
import net.xzos.upgradeall.wrapper.download.DownloadTasker

class FileHubListItemHandler(private val activity: AppCompatActivity) : RecyclerViewHandler() {
    fun showDialog(fileTasker: DownloadTasker) {
        TaskerListDialog.newInstance(activity, fileTasker)
    }
}