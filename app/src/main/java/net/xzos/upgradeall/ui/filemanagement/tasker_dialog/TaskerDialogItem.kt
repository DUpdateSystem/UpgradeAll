package net.xzos.upgradeall.ui.filemanagement.tasker_dialog

import android.content.Context
import net.xzos.upgradeall.core.installer.FileType
import net.xzos.upgradeall.ui.base.list.ListItemView
import net.xzos.upgradeall.ui.filemanagement.DownloadTaskerNumUtil
import net.xzos.upgradeall.wrapper.download.DownloadTasker

class TaskerDialogItem(val fileTasker: DownloadTasker) : ListItemView {
    private val numUtil = DownloadTaskerNumUtil(fileTasker.rustDownloader)
    val suspendable: Boolean = numUtil.getDownloadingNum() > 0
    val continuable: Boolean = numUtil.getPauseNum() > 0
    val retryable: Boolean = numUtil.getFailedNum() > 0
    var fileType: FileType? = null

    fun init(context: Context) {
        fileType = fileTasker.fileType
    }
}