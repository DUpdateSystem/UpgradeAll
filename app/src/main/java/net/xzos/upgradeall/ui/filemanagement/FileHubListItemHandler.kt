package net.xzos.upgradeall.ui.filemanagement

import android.content.Context
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.xzos.upgradeall.core.downloader.Downloader
import net.xzos.upgradeall.ui.base.recycleview.RecyclerViewHandler
import net.xzos.upgradeall.ui.filemanagement.tasker_dialog.TaskerListDialog
import net.xzos.upgradeall.utils.runUiFun

class FileHubListItemHandler(private val context: Context) : RecyclerViewHandler() {
    fun showDialog(downloader: Downloader?) {
        downloader?.run {
            GlobalScope.launch {
                val downloadList = getDownloadList()
                runUiFun { TaskerListDialog(context, downloadList).show() }
            }
        }
    }
}