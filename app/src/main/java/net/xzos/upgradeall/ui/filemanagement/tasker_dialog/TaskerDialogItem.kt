package net.xzos.upgradeall.ui.filemanagement.tasker_dialog

import android.content.Context
import kotlinx.coroutines.runBlocking
import net.xzos.upgradeall.core.installer.FileType
import net.xzos.upgradeall.ui.base.list.ListItemView
import net.xzos.upgradeall.ui.filemanagement.DownloadTaskerNumUtil
import net.xzos.upgradeall.wrapper.download.FileTaskerWrapper

class TaskerDialogItem(val fileTasker: FileTaskerWrapper) : ListItemView {
    private val numUtil = DownloadTaskerNumUtil(fileTasker.downloader)
    val suspendable: Boolean = runBlocking { numUtil.getDownloadingNum() } > 0
    val continuable: Boolean = runBlocking { numUtil.getPauseNum() } > 0
    val retryable: Boolean = runBlocking { numUtil.getFailedNum() } > 0
    var fileType: FileType? = null

    fun init(context: Context) {
        fileType = fileTasker.fileType
    }
}