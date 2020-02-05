package net.xzos.upgradeAll.server.update

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
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import net.xzos.upgradeAll.R
import net.xzos.upgradeAll.application.MyApplication.Companion.context
import net.xzos.upgradeAll.server.app.manager.AppManager
import net.xzos.upgradeAll.server.app.manager.module.App
import net.xzos.upgradeAll.server.app.manager.module.Updater
import net.xzos.upgradeAll.ui.activity.MainActivity
import net.xzos.upgradeAll.ui.viewmodels.componnent.EditIntPreference
import net.xzos.upgradeAll.utils.MiscellaneousUtils
import java.util.concurrent.Executors

object UpdateManager {
    private val apps: HashSet<App>
        get() = AppManager.getApps()
    private val jobMap = hashMapOf<App, Job>()
    private fun cancelJob(app: App) {
        jobMap.remove(app)
        if (jobMap.isEmpty()) finishCheckUpdate()
    }

    private val mutex = Mutex()

    private val needUpdateAppList = hashSetOf<App>()
    private val executorCoroutineDispatcher = Executors.newCachedThreadPool().asCoroutineDispatcher()
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
    fun blockRenewAll(): HashSet<App> {
        runBlocking {
            // 尝试刷新全部软件
            renewAll()
            // 等待刷新完成
            mutex.lock()
            mutex.unlock()
        }
        return needUpdateAppList
    }

    fun renewAll() {
        runBlocking {
            mutex.lock()
        }
        startUpdateNotification()
        needUpdateAppList.clear()
        for (appId in apps) {
            startJob(appId)
        }
    }

    fun renewApp(app: App): Boolean {
        cancelJob(app)
        return runBlocking(executorCoroutineDispatcher) {
            Updater(app).isSuccessRenew()
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
            if (Updater(app).getUpdateStatus() == Updater.APP_OUTDATED)
                needUpdateAppList.add(app)
            cancelJob(app)
            if (jobMap.isEmpty()) {
                finishCheckUpdate()
                if (mutex.isLocked)
                    mutex.unlock()
            } else updateStatusNotification()
        }
    }

    private fun startUpdateNotification() {
        NotificationManagerCompat.from(context).apply {
            builder.setContentTitle("UpgradeAll 更新服务运行中")
                    .setContentText(null)
                    .setProgress(0, 0, false)
                    .setOngoing(true)
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
            val defaultBackgroundSyncTime = context.resources.getInteger(R.integer.default_background_sync_data_time)  // 默认自动刷新时间 18h
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
