package net.xzos.upgradeall.ui.filemanagement.tasker_dialog

import android.view.View
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.xzos.upgradeall.core.filetasker.FileTasker
import net.xzos.upgradeall.server.downloader.installFileTasker

class TaskerDialogHandler(private val fileTasker: FileTasker, private val dialog: TaskerListDialog) {
    fun install() {
        GlobalScope.launch {
            installFileTasker(fileTasker)
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
        fileTasker.cancel()
        dialog.cancel()
    }

    fun open(view: View) {
        fileTasker.openDownloadDir(view.context)
    }
}