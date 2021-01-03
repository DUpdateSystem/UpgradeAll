package net.xzos.upgradeall.server.downloader

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.runBlocking
import net.xzos.upgradeall.application.MyApplication
import net.xzos.upgradeall.core.downloader.DownloadId
import net.xzos.upgradeall.core.downloader.DownloaderManager

class DownloadBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val downloaderIdString = intent.getStringExtra(EXTRA_IDENTIFIER_DOWNLOADER_ID) ?: return
        val downloaderId = DownloadId.parsingIdString(downloaderIdString)
        val downloader = DownloaderManager.getDownloader(downloaderId) ?: return
        when (intent.getIntExtra(EXTRA_IDENTIFIER_DOWNLOAD_CONTROL, -1)) {
            DOWNLOAD_CANCEL -> downloader.cancel()
            DOWNLOAD_RETRY -> downloader.retry()
            DOWNLOAD_PAUSE -> downloader.pause()
            DOWNLOAD_CONTINUE -> downloader.resume()
            INSTALL_APK -> runBlocking { downloader.fileAsset.install({}, {}) }
            SAVE_FILE -> TODO("Save file to external")
        }
    }

    companion object {
        internal val ACTION_SNOOZE = "${MyApplication.context.packageName}.DOWNLOAD_BROADCAST"

        internal const val EXTRA_IDENTIFIER_DOWNLOADER_ID = "DOWNLOADER_ID"

        internal const val EXTRA_IDENTIFIER_DOWNLOAD_CONTROL = "DOWNLOAD_CONTROL"
        internal const val DOWNLOAD_CANCEL = 1
        internal const val DOWNLOAD_RETRY = 2
        internal const val DOWNLOAD_PAUSE = 3
        internal const val DOWNLOAD_CONTINUE = 4
        internal const val SAVE_FILE = 10
        internal const val INSTALL_APK = 11
    }
}
