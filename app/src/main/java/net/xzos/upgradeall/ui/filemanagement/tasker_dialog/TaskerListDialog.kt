package net.xzos.upgradeall.ui.filemanagement.tasker_dialog

import android.app.Dialog
import android.os.Bundle
import com.tonyodev.fetch2.Download
import kotlinx.coroutines.runBlocking
import net.xzos.upgradeall.R
import net.xzos.upgradeall.core.downloader.Downloader
import net.xzos.upgradeall.databinding.DialogFileTaskerBinding
import net.xzos.upgradeall.ui.base.listdialog.ListDialog
import java.io.File

class TaskerListDialog(
        downloader: Downloader
) : ListDialog(
        R.string.show_download_tasker,
        TaskerListAdapter(runBlocking { downloader.getDownloadList().map { getItemView(it) } })
) {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val contentBinding = DialogFileTaskerBinding.inflate(layoutInflater)
        initBinding(contentBinding.listLayout)
        return initDialog(contentBinding.root)
    }

    companion object {
        private fun getItemView(downloader: Download): TaskerItem {
            val file = File(downloader.file)
            return TaskerItem(file.name, file.path, downloader.progress)
        }
    }
}