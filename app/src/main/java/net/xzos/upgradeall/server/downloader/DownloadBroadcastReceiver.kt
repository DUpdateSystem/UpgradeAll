package net.xzos.upgradeall.server.downloader

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.xzos.upgradeall.application.MyApplication
import net.xzos.upgradeall.wrapper.download.DownloadTaskerManager
import net.xzos.upgradeall.wrapper.download.install

class DownloadBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val fileTaskerId = intent.getStringExtra(EXTRA_IDENTIFIER_FILE_TASKER_ID) ?: return
        val fileTasker = DownloadTaskerManager.getFileTasker(fileTaskerId) ?: return
        val downloader = fileTasker.rustDownloader ?: return
        val notification = DownloadNotificationManager.getNotification(fileTasker) ?: return
        when (intent.getIntExtra(EXTRA_IDENTIFIER_FILE_TASKER_CONTROL, -1)) {
            DOWNLOAD_RETRY -> downloader.retry()
            DOWNLOAD_PAUSE -> downloader.pause()
            DOWNLOAD_CONTINUE -> downloader.resume()
            DOWNLOAD_CANCEL -> downloader.cancel()
            NOTIFY_CANCEL -> notification.cancelNotification()
            INSTALL_APK -> GlobalScope.launch {
                fileTasker.install()
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