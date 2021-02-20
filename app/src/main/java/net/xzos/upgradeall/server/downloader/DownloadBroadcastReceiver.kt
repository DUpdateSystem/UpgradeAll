package net.xzos.upgradeall.server.downloader

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.runBlocking
import net.xzos.upgradeall.application.MyApplication
import net.xzos.upgradeall.core.filetasker.FileTaskerManager

class DownloadBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val fileTaskerId = intent.getIntExtra(EXTRA_IDENTIFIER_FILE_TASKER_ID, -1)
        val fileTasker = FileTaskerManager.getFileTasker(fileTaskerId) ?: return
        when (intent.getIntExtra(EXTRA_IDENTIFIER_FILE_TASKER_CONTROL, -1)) {
            DOWNLOAD_CANCEL -> {
                fileTasker.cancel()
                val notificationId = intent.getIntExtra(EXTRA_IDENTIFIER_NOTIFICATION_ID, -1)
                NotificationManagerCompat.from(context).cancel(notificationId)
            }
            DOWNLOAD_RETRY -> fileTasker.retry()
            DOWNLOAD_PAUSE -> fileTasker.pause()
            DOWNLOAD_CONTINUE -> fileTasker.resume()
            INSTALL_APK -> runBlocking { fileTasker.install({}, {}) }
            SAVE_FILE -> TODO("Save file to external")
        }
    }

    companion object {
        internal val ACTION_SNOOZE = "${MyApplication.context.packageName}.DOWNLOAD_BROADCAST"

        internal const val EXTRA_IDENTIFIER_NOTIFICATION_ID = "EXTRA_IDENTIFIER_NOTIFICATION_ID"
        internal const val EXTRA_IDENTIFIER_FILE_TASKER_ID = "FILE_TASKER_ID"

        internal const val EXTRA_IDENTIFIER_FILE_TASKER_CONTROL = "FILE_TASKER_CONTROL"
        internal const val DOWNLOAD_CANCEL = 1
        internal const val DOWNLOAD_RETRY = 2
        internal const val DOWNLOAD_PAUSE = 3
        internal const val DOWNLOAD_CONTINUE = 4
        internal const val SAVE_FILE = 10
        internal const val INSTALL_APK = 11
    }
}
