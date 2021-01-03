package net.xzos.upgradeall.core.downloader

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.tonyodev.fetch2.Fetch
import com.tonyodev.fetch2.FetchConfiguration
import net.xzos.upgradeall.core.coreConfig


internal class DownloadService : Service() {

    override fun onCreate() {
        super.onCreate()
        renewFetch()
        notificationMaker?.run {
            val (notificationId, notification) = this()
            startForeground(notificationId, notification)
        }
    }

    private fun renewFetch() {
        val fetchConfiguration = FetchConfiguration.Builder(coreConfig.androidContext)
                .setDownloadConcurrentLimit(coreConfig.download_max_task_num)
                .setHttpDownloader(getDownloader())
                .build()
        fetch = Fetch.Impl.getInstance(fetchConfiguration).apply {
            addListener(DownloadRegister)
            removeAll()
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    companion object {
        private var service: Service? = null
        private var fetch: Fetch? = null

        private var notificationMaker: (() -> Pair<Int, Notification>)? = null

        fun setNotificationMaker(
                notificationMaker: () -> Pair<Int, Notification>
        ) {
            this.notificationMaker = notificationMaker
        }

        fun getFetch(): Fetch {
            if (fetch == null)
                startService(coreConfig.androidContext)
            return fetch!!
        }

        fun close() {
            fetch?.close()
            fetch = null
            service?.stopSelf()

        }

        private fun startService(context: Context) {
            val intent = Intent(context, DownloadService::class.java)
            context.startService(intent)
        }
    }
}