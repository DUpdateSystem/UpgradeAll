package net.xzos.upgradeall.utils.downloader

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import kotlinx.coroutines.runBlocking
import net.xzos.upgradeall.core.oberver.Observer
import net.xzos.upgradeall.utils.downloader.AriaRegister.getCancelNotifyKey
import net.xzos.upgradeall.utils.downloader.AriaRegister.getCompleteNotifyKey

class AriaDownloadService : Service() {

    override fun onCreate() {
        super.onCreate()
        val (notificationId, notification) = DownloadNotification.getDownloadServiceNotification()
        startForeground(notificationId, notification)
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            stopSelf(startId)
            return START_REDELIVER_INTENT
        }
        val url = intent.getStringExtra(URL) as String
        val fileName = intent.getStringExtra(FILE_NAME) as String
        val headers = intent.getSerializableExtra(HEADERS) as HashMap<String, String>
        val ariaDownloader = AriaDownloader(url)
        val file = runBlocking { ariaDownloader.start(fileName, headers) }
        if (file != null) {
            register(startId, url)
        } else {
            stopSelf(startId)
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun register(startId: Int, url: String) {
        val observer = object : Observer {
            override fun onChanged(vars: Array<out Any>): Any? {
                AriaRegister.removeObserver(this)
                stopSelf(startId)
                return null
            }
        }
        AriaRegister.observeForever(url.getCompleteNotifyKey(), observer)
        AriaRegister.observeForever(url.getCancelNotifyKey(), observer)
    }

    companion object {
        private const val URL = "URL"
        private const val FILE_NAME = "FILE_NAME"
        private const val HEADERS = "HEADERS"

        fun startService(context: Context, url: String, fileName: String, headers: HashMap<String, String>) {
            val intent = Intent(context, AriaDownloadService::class.java).apply {
                putExtra(URL, url)
                putExtra(FILE_NAME, fileName)
                putExtra(HEADERS, headers)
            }
            context.startService(intent)
        }
    }
}
