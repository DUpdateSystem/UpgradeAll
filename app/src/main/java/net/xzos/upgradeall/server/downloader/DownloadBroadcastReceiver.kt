package net.xzos.upgradeall.server.downloader

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.xzos.upgradeall.application.MyApplication
import net.xzos.upgradeall.core.downloader.filetasker.FileTaskerManager
import net.xzos.upgradeall.wrapper.download.fileType
import net.xzos.upgradeall.wrapper.download.installFileTasker

class DownloadBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val fileTaskerId = intent.getStringExtra(EXTRA_IDENTIFIER_FILE_TASKER_ID) ?: return
        val fileTasker = FileTaskerManager.getFileTasker(idString = fileTaskerId) ?: return
        val notification = DownloadNotificationManager.getNotification(fileTaskerId) ?: return
        when (intent.getIntExtra(EXTRA_IDENTIFIER_FILE_TASKER_CONTROL, -1)) {
            DOWNLOAD_RETRY -> fileTasker.retry()
            DOWNLOAD_PAUSE -> fileTasker.pause()
            DOWNLOAD_CONTINUE -> fileTasker.resume()
            DOWNLOAD_CANCEL -> {
                notification.cancelNotification()
                fileTasker.cancel()
            }
            NOTIFY_CANCEL -> notification.cancelNotification()
            INSTALL_APK -> GlobalScope.launch {
                installFileTasker(
                    context, fileTasker,
                    fileTasker.fileType(context) ?: return@launch,
                    notification
                )
            }
            OPEN_FILE -> Log.i("Download", "open file: TODO")
        }
    }

    companion object {
        internal val ACTION_SNOOZE = "${MyApplication.context.packageName}.DOWNLOAD_BROADCAST"

        internal const val EXTRA_IDENTIFIER_FILE_TASKER_ID = "FILE_TASKER_ID"

        internal const val EXTRA_IDENTIFIER_FILE_TASKER_CONTROL = "FILE_TASKER_CONTROL"
        internal const val DOWNLOAD_CANCEL = 1
        internal const val DOWNLOAD_RETRY = 2
        internal const val DOWNLOAD_PAUSE = 3
        internal const val DOWNLOAD_CONTINUE = 4
        internal const val OPEN_FILE = 10
        internal const val INSTALL_APK = 11
        internal const val NOTIFY_CANCEL = 5
    }
}