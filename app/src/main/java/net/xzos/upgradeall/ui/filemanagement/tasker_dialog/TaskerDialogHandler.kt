package net.xzos.upgradeall.ui.filemanagement.tasker_dialog

import android.content.Context
import android.view.View
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.xzos.upgradeall.core.downloader.filetasker.FileTasker
import net.xzos.upgradeall.server.downloader.deleteFileTasker
import net.xzos.upgradeall.server.downloader.installFileTasker

class TaskerDialogHandler(
        private val fileTasker: FileTasker, private val dialog: TaskerListDialog,
        private val context: Context
) {
    fun install() {
        GlobalScope.launch {
            installFileTasker(fileTasker, context)
        }
    }

    fun pause() {
        fileTasker.pause()
    }

    fun resume() {
        fileTasker.resume()
    }

    fun retry() {
        fileTasker.retry()
    }

    fun delete() {
        deleteFileTasker(fileTasker)
        dialog.cancel()
    }

    fun open(view: View) {
        // TODO: 打开文件
    }
}