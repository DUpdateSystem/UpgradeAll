package net.xzos.upgradeall.ui.filemanagement

import androidx.fragment.app.FragmentManager
import net.xzos.upgradeall.core.downloader.Downloader
import net.xzos.upgradeall.ui.base.recycleview.RecyclerViewHandler
import net.xzos.upgradeall.ui.filemanagement.tasker_dialog.TaskerListDialog

class FileHubListItemHandler(private val supportFragmentManager: FragmentManager) : RecyclerViewHandler(
) {
    fun showDialog(downloader: Downloader?) {
        downloader?.run { TaskerListDialog(this).show(supportFragmentManager) }
    }
}