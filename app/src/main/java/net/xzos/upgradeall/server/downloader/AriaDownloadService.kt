package net.xzos.upgradeall.server.downloader

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.tonyodev.fetch2.Download
import kotlinx.coroutines.runBlocking
import net.xzos.upgradeall.core.oberver.ObserverFun
import net.xzos.upgradeall.server.downloader.AriaRegister.getCancelNotifyKey
import net.xzos.upgradeall.server.downloader.AriaRegister.getCompleteNotifyKey

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

        @Suppress("UNCHECKED_CAST")
        val headers = intent.getSerializableExtra(HEADERS) as HashMap<String, String>
        val ariaDownloader = AriaDownloader(url, this)
        try {
            runBlocking {
                ariaDownloader.start(fileName, headers, fun(downloadId) {
                    register(startId, downloadId)
                })
            }
        } catch (ignore: Throwable) {
            stopSelf(startId)
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun register(startId: Int, downloadId: Int) {
        val observerFun: ObserverFun<Download> = fun(_) {
            stopSelf(startId)
            OBSERVER_FUN_MAP.remove(startId)?.let {
                AriaRegister.removeObserver(it)
            }
        }
        OBSERVER_FUN_MAP[startId] = observerFun
        AriaRegister.observeForever(downloadId.getCompleteNotifyKey(), observerFun)
        AriaRegister.observeForever(downloadId.getCancelNotifyKey(), observerFun)
    }

    companion object {
        private const val URL = "URL"
        private const val FILE_NAME = "FILE_NAME"
        private const val HEADERS = "HEADERS"

        private val OBSERVER_FUN_MAP: HashMap<Int, ObserverFun<Download>> = hashMapOf()

        fun startService(context: Context, url: String, fileName: String, headers: Map<String, String>) {
            val intent = Intent(context, AriaDownloadService::class.java).apply {
                putExtra(URL, url)
                putExtra(FILE_NAME, fileName)
                putExtra(HEADERS, HashMap(headers))
            }
            context.startService(intent)
        }
    }
}
