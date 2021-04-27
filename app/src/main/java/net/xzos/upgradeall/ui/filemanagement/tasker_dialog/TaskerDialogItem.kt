package net.xzos.upgradeall.ui.filemanagement.tasker_dialog

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import net.xzos.upgradeall.core.filetasker.FileTasker
import net.xzos.upgradeall.ui.base.list.ListItemView
import net.xzos.upgradeall.ui.filemanagement.DownloadTaskerNumUtil

class TaskerDialogItem(
        val fileTasker: FileTasker,
        context: Context
) : ListItemView {
    private val numUtil = DownloadTaskerNumUtil(fileTasker.downloader)
    val installable: Boolean = runBlocking(Dispatchers.IO) { fileTasker.isInstallable(context) }
    val suspendable: Boolean = runBlocking { numUtil.getDownloadingNum() } > 0
    val continuable: Boolean = runBlocking { numUtil.getPauseNum() } > 0
    val retryable: Boolean = runBlocking { numUtil.getFailedNum() } > 0
}