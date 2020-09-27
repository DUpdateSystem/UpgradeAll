package net.xzos.upgradeall.server.downloader

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import net.xzos.upgradeall.application.MyApplication

class DownloadBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val downloaderId = intent.getIntExtra(EXTRA_IDENTIFIER_DOWNLOADER_URL, 0)
        val downloader = AriaDownloader.getDownloader(downloaderId) ?: return
        when (intent.getIntExtra(EXTRA_IDENTIFIER_DOWNLOAD_CONTROL, -1)) {
            DOWNLOAD_CANCEL -> downloader.delTask()
            DOWNLOAD_RESTART -> downloader.restart()
            DOWNLOAD_PAUSE -> downloader.pause()
            DOWNLOAD_CONTINUE -> downloader.resume()
            INSTALL_APK -> downloader.install()
            SAVE_FILE -> downloader.saveFile()
        }
    }

    companion object {
        internal val ACTION_SNOOZE = "${MyApplication.context.packageName}.DOWNLOAD_BROADCAST"

        internal const val EXTRA_IDENTIFIER_DOWNLOADER_URL = "DOWNLOADER_URL"

        internal const val EXTRA_IDENTIFIER_DOWNLOAD_CONTROL = "DOWNLOAD_CONTROL"
        internal const val DOWNLOAD_CANCEL = 1
        internal const val DOWNLOAD_RESTART = 2
        internal const val DOWNLOAD_PAUSE = 3
        internal const val DOWNLOAD_CONTINUE = 4
        internal const val SAVE_FILE = 10
        internal const val INSTALL_APK = 11
    }
}
