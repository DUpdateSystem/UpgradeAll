package net.xzos.upgradeall.core.downloader.service

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.tonyodev.fetch2.Fetch
import com.tonyodev.fetch2.FetchConfiguration
import kotlinx.coroutines.runBlocking
import net.xzos.upgradeall.core.downloader.downloadConfig
import net.xzos.upgradeall.core.downloader.filedownloader.fetch.getDownloader
import net.xzos.upgradeall.core.downloader.filedownloader.observe.DownloadRegister
import net.xzos.upgradeall.core.utils.coroutines.CoroutinesCount
import net.xzos.upgradeall.core.utils.coroutines.ValueLock


internal class DownloadService : Service() {

    override fun onCreate() {
        super.onCreate()
        renewFetch()
        notificationMaker?.run {
            val (notificationId, notification) = this()
            startForeground(notificationId, notification)
        }
        getService = fun() = this
    }

    override fun onDestroy() {
        fetchValue.refresh()
        getService = fun() = null
        coroutinesCount.down()
        super.onDestroy()
    }

    private fun renewFetch() {
        val fetchConfiguration = FetchConfiguration.Builder(this)
            .setDownloadConcurrentLimit(downloadConfig.DOWNLOAD_MAX_TASK_NUM)
            .setHttpDownloader(getDownloader())
            .build()
        val fetch = Fetch.Impl.getInstance(fetchConfiguration).apply {
            addListener(DownloadRegister)
            removeAll()
        }
        fetchValue.setValue(fetch)
        coroutinesCount.up()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    companion object {
        private var getService: () -> Service? = fun() = null
        private val fetchValue = ValueLock<Fetch>()
        private val coroutinesCount = CoroutinesCount(0)

        private var notificationMaker: (() -> Pair<Int, Notification>)? = null

        fun setNotificationMaker(notificationMaker: () -> Pair<Int, Notification>) {
            Companion.notificationMaker = notificationMaker
        }

        suspend fun getFetch(): Fetch {
            if (fetchValue.isEmpty()) {
                startService(downloadConfig.ANDROID_CONTEXT)
            }
            return fetchValue.getValue()!!
        }

        fun close() {
            runBlocking { fetchValue.getValue(false)?.close() }
            getService()?.stopSelf()

        }

        private fun startService(context: Context) {
            val intent = Intent(context, DownloadService::class.java)
            context.startService(intent)
        }
    }
}