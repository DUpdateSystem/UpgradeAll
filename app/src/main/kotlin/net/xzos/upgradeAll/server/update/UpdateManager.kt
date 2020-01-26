package net.xzos.upgradeAll.server.update

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.*
import net.xzos.upgradeAll.R
import net.xzos.upgradeAll.application.MyApplication.Companion.context
import net.xzos.upgradeAll.server.app.manager.AppManager
import net.xzos.upgradeAll.server.app.manager.module.Updater
import java.util.concurrent.Executors

object UpdateManager {
    private val appIds = AppManager.getAppIds()
    private val jobMapLiveData = MutableLiveData(hashMapOf<Long, Job>())
    private val executorCoroutineDispatcher = Executors.newCachedThreadPool().asCoroutineDispatcher()
    private const val CHANNEL_ID = "UpdateServiceNotification"
    private const val updateNotification = 0
    private val builder = NotificationCompat.Builder(context, CHANNEL_ID).apply {
        setContentTitle("UpgradeAll 更新服务运行中")
        setOngoing(true)
        setSmallIcon(R.drawable.ic_launcher_foreground)
        priority = NotificationCompat.PRIORITY_LOW
    }

    init {
        createNotificationChannel()
        GlobalScope.launch(Dispatchers.Main) {
            jobMapLiveData.observeForever {
                if (it.isEmpty()) initUpdateNotification()
                else checkUpdateStatusNotification()
            }
        }
    }

    fun renewAll() {
        GlobalScope.launch {
            for (appId in appIds) {
                startJob(appId)
            }
        }
        checkUpdateStatusNotification()
    }

    fun renewApp(appId: Long): Boolean {
        jobMapLiveData.value?.also {
            it[appId]?.cancel()
            it.remove(appId)
        }
        runBlocking(Dispatchers.Main) {
            jobMapLiveData.notifyObserver()
        }
        return runBlocking(executorCoroutineDispatcher) {
            Updater(AppManager.getApp(appId).engine).isSuccessRenew()
        }
    }

    private suspend fun startJob(appId: Long) {
        jobMapLiveData.value?.let {
            it[appId] = GlobalScope.launch(Dispatchers.IO) {
                Updater(AppManager.getApp(appId).engine).isSuccessRenew()
                it.remove(appId)
                withContext(Dispatchers.Main) {
                    jobMapLiveData.notifyObserver()
                }
            }
            withContext(Dispatchers.Main) {
                jobMapLiveData.notifyObserver()
            }
        }
    }

    private fun initUpdateNotification() {
        NotificationManagerCompat.from(context).apply {
            builder.setContentTitle("UpgradeAll 更新服务运行中")
                    .setContentText(null)
                    .setProgress(0, 0, false)
                    .setOngoing(true)
        }
        notificationNotify()
    }

    private fun checkUpdateStatusNotification() {
        val appNum = AppManager.getAppIds().size
        val renewedNum = appNum - (jobMapLiveData.value?.size ?: 0)
        NotificationManagerCompat.from(context).apply {
            builder.setContentTitle("检查更新中")
                    .setContentText("后台任务: $renewedNum/$appNum")
                    .setProgress(appNum, renewedNum, false)
        }
        notificationNotify()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "更新服务", NotificationManager.IMPORTANCE_MIN)
            channel.description = "显示更新服务状态"
            channel.enableLights(false)
            channel.enableVibration(false)
            channel.setShowBadge(true)
            val notificationManager = context.getSystemService(
                    Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun notificationNotify() {
        NotificationManagerCompat.from(context).notify(updateNotification, builder.build())
    }

    /**
     * 拓展 LiveData 监听列表元素添加、删除操作的支持
     */
    private fun <T> MutableLiveData<T>.notifyObserver() {
        this.value = this.value
    }
}
