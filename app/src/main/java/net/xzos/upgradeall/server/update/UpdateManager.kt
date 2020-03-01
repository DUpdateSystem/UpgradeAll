package net.xzos.upgradeall.server.update

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.xzos.dupdatesystem.core.data.config.AppConfig
import net.xzos.dupdatesystem.core.server_manager.AppManager
import net.xzos.dupdatesystem.core.server_manager.module.app.App
import net.xzos.dupdatesystem.core.server_manager.module.app.Updater
import net.xzos.upgradeall.R
import net.xzos.upgradeall.application.MyApplication.Companion.context
import net.xzos.upgradeall.ui.activity.MainActivity
import net.xzos.upgradeall.ui.viewmodels.componnent.EditIntPreference
import net.xzos.upgradeall.utils.MiscellaneousUtils


object UpdateManager {
    private val apps: List<App>
        get() = AppManager.apps
    private val jobMap = hashMapOf<App, Job>()
    private suspend fun finishJob(app: App) {
        jobMapMutex.withLock {
            jobMap.remove(app)
            if (jobMap.isEmpty()) {
                finishCheckUpdate()
                if (flashMutex.isLocked) flashMutex.unlock()
            } else updateStatusNotification()
        }
    }

    private val jobMapMutex = Mutex()

    private val flashMutex = Mutex()

    private val needUpdateAppList = mutableListOf<App>()
    private const val CHANNEL_ID = "UpdateServiceNotification"
    private const val updateNotificationId = 0

    private val builder = NotificationCompat.Builder(context, CHANNEL_ID).apply {
        setContentTitle("UpgradeAll 更新服务运行中")
        setOngoing(true)
        setSmallIcon(R.drawable.ic_launcher_foreground)
        priority = NotificationCompat.PRIORITY_LOW
    }

    init {
        UpdateServiceReceiver.initAlarms()
        createNotificationChannel()
    }

    // 刷新所有软件并等待，返回需要更新的软件数量
    suspend fun blockRenewAll(): List<App> {
        if (!flashMutex.isLocked) {
            // 尝试刷新全部软件
            renewAll()
        }
        // 等待刷新完成
        waitRenew()
        return needUpdateAppList
    }

    private suspend fun waitRenew() {
        flashMutex.lock()
        flashMutex.unlock()
    }

    fun renewAll() {
        GlobalScope.launch {
            flashMutex.lock()
            startUpdateNotification()
            needUpdateAppList.clear()
            for (appId in apps) {
                startJob(appId)
            }
        }
    }

    private fun finishCheckUpdate() {
        if (needUpdateAppList.isNotEmpty())
            updateNotification(needUpdateAppList.size)
        else
            NotificationManagerCompat.from(context).cancel(updateNotificationId)
    }

    private fun startJob(app: App) {
        jobMap[app] = GlobalScope.launch(Dispatchers.IO) {
            when (Updater(app).getUpdateStatus()) {
                Updater.APP_OUTDATED -> needUpdateAppList.add(app)
                Updater.NETWORK_404 -> app.renew()
            }
            finishJob(app)
        }
    }

    private fun startUpdateNotification() {
        NotificationManagerCompat.from(context).apply {
            builder.setContentTitle("UpgradeAll 更新服务运行中")
                    .setContentText(null)
                    .setProgress(0, 0, false)
                    // TODO: 实现完整的后台更新后应修改为 false，使应用常驻
                    .setOngoing(false)
        }
        notificationNotify()
    }

    private fun updateStatusNotification() {
        val appNum = apps.size
        val renewedNum = appNum - jobMap.size
        NotificationManagerCompat.from(context).apply {
            builder.setContentTitle("检查更新中")
                    .setContentText("后台任务: $renewedNum/$appNum")
                    .setProgress(appNum, renewedNum, false)
                    // 如果运行正常，此处应该不可消除（
                    // 未知 bug，暂时允许用户消除通知
                    // TODO: 实现完整的后台更新后应再次确认此处
                    .setOngoing(false)
        }
        notificationNotify()
    }

    private fun updateNotification(needUpdateAppNum: Int) {
        val resultIntent = Intent(context, MainActivity::class.java)
        val resultPendingIntent: PendingIntent? = TaskStackBuilder.create(context).run {
            addNextIntentWithParentStack(resultIntent)
            getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
        }

        NotificationManagerCompat.from(context).apply {
            builder.run {
                setContentTitle("$needUpdateAppNum 个应用需要更新")
                setProgress(0, 0, false)
                setOngoing(false)
                if (!MiscellaneousUtils.isBackground()) {
                    setContentText("点按打开应用主页")
                    setContentIntent(resultPendingIntent)
                } else
                    setContentText(null)
            }
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
        NotificationManagerCompat.from(context).notify(updateNotificationId, builder.build())
    }
}

class UpdateServiceReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        UpdateManager.renewAll()
    }

    companion object {
        private val ACTION_SNOOZE = "${context.packageName}.UPDATE_SERVICE_BROADCAST"
        fun initAlarms() {
            val defaultBackgroundSyncTime = AppConfig.default_background_sync_data_time  // 默认自动刷新时间 18h
            val alarmTime: Long = EditIntPreference.getInt("background_sync_time", defaultBackgroundSyncTime).toLong() * 60 * 60 * 1000
            val alarmIntent = PendingIntent.getBroadcast(context, 0,
                    Intent(context, UpdateServiceReceiver::class.java).apply {
                        action = ACTION_SNOOZE
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT)
            (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager)
                    .setInexactRepeating(
                            AlarmManager.ELAPSED_REALTIME_WAKEUP,
                            SystemClock.elapsedRealtime() + alarmTime,
                            alarmTime,
                            alarmIntent
                    )
        }

    }
}
