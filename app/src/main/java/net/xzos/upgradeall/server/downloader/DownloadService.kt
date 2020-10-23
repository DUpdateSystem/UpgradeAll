package net.xzos.upgradeall.server.downloader

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tonyodev.fetch2.Download
import net.xzos.upgradeall.core.data.json.gson.DownloadInfoItem
import net.xzos.upgradeall.core.oberver.ObserverFun
import net.xzos.upgradeall.server.downloader.DownloadRegister.getCancelNotifyKey
import net.xzos.upgradeall.server.downloader.DownloadRegister.getCompleteNotifyKey

class DownloadService : Service() {

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
        val json = intent.getStringExtra(DOWNLOAD_INFO_LIST)
        val taskName = intent.getStringExtra(DOWNLOAD_TASK_NAME)!!
        val listType = object : TypeToken<ArrayList<DownloadInfoItem?>?>() {}.type
        val downloadInfoList: List<DownloadInfoItem> = Gson().fromJson(json, listType)
        val downloader = Downloader(this)
        for (downloadInfo in downloadInfoList) {
            downloader.addTask(downloadInfo.name, downloadInfo.url, downloadInfo.headers, downloadInfo.cookies)
        }
        downloader.start(taskName, fun(downloadId) {
            register(startId, downloadId)
        })
        return super.onStartCommand(intent, flags, startId)
    }

    private fun register(startId: Int, downloadId: Int) {
        val observerFun: ObserverFun<Download> = fun(_) {
            stopSelf(startId)
            OBSERVER_FUN_MAP.remove(startId)?.let {
                DownloadRegister.removeObserver(it)
            }
        }
        OBSERVER_FUN_MAP[startId] = observerFun
        DownloadRegister.observeForever(downloadId.getCompleteNotifyKey(), observerFun)
        DownloadRegister.observeForever(downloadId.getCancelNotifyKey(), observerFun)
    }

    companion object {
        private const val DOWNLOAD_INFO_LIST = "DOWNLOAD_TASK_INFO_DICT"
        private const val DOWNLOAD_TASK_NAME = "DOWNLOAD_TASK_NAME"

        private val OBSERVER_FUN_MAP: HashMap<Int, ObserverFun<Download>> = hashMapOf()

        fun startService(taskName: String, downloadInfoList: List<DownloadInfoItem>, context: Context) {
            val intent = Intent(context, DownloadService::class.java).apply {
                putExtra(DOWNLOAD_TASK_NAME, taskName)
                putExtra(DOWNLOAD_INFO_LIST, Gson().toJson(downloadInfoList))
            }
            context.startService(intent)
        }
    }
}
