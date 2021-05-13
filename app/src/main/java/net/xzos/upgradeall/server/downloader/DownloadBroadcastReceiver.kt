package net.xzos.upgradeall.server.downloader

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.runBlocking
import net.xzos.upgradeall.application.MyApplication
import net.xzos.upgradeall.core.filetasker.FileTaskerManager

class DownloadBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val fileTaskerId = intent.getIntExtra(EXTRA_IDENTIFIER_FILE_TASKER_ID, -1)
        val fileTasker = FileTaskerManager.getFileTasker(fileTaskerId) ?: return
        when (intent.getIntExtra(EXTRA_IDENTIFIER_FILE_TASKER_CONTROL, -1)) {
            DOWNLOAD_CANCEL -> deleteFileTasker(fileTasker)
            DOWNLOAD_RETRY -> fileTasker.retry()
            DOWNLOAD_PAUSE -> fileTasker.pause()
            DOWNLOAD_CONTINUE -> fileTasker.resume()
            NOTIFY_CANCEL -> DownloadNotificationManager.getNotification(fileTasker)
                ?.cancelNotification()
            INSTALL_APK -> runBlocking { installFileTasker(fileTasker, context) }
            OPEN_FILE -> fileTasker.openDownloadDir(context)
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