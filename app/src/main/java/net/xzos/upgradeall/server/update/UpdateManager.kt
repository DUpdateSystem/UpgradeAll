package net.xzos.upgradeall.server.update

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.xzos.upgradeall.R
import net.xzos.upgradeall.application.MyApplication.Companion.context
import net.xzos.upgradeall.core.oberver.Observer
import net.xzos.upgradeall.core.server_manager.UpdateManager
import net.xzos.upgradeall.ui.activity.MainActivity
import net.xzos.upgradeall.utils.MiscellaneousUtils

object UpdateManager {
    private const val CHANNEL_ID = "UpdateServiceNotification"
    private const val updateNotificationId = 0

    private val builder = NotificationCompat.Builder(context, CHANNEL_ID).apply {
        setContentTitle("UpgradeAll 更新服务运行中")
        setOngoing(true)
        setSmallIcon(R.drawable.ic_launcher_foreground)
        priority = NotificationCompat.PRIORITY_LOW
    }

    init {
        createNotificationChannel()
        UpdateManager.observeForever(object : Observer {
            override fun onChanged(vararg vars: Any): Any? {
                return getNotify()
            }
        })
    }

    private fun getNotify() {
        val allAppsNum = UpdateManager.apps.size
        val finishedAppNum = UpdateManager.finishedAppNum.toInt()
        if (finishedAppNum != allAppsNum) {
            updateStatusNotification(allAppsNum, finishedAppNum)
        } else {
            val needUpdateAppNum = runBlocking { UpdateManager.getNeedUpdateAppList(block = false).size }
            if (needUpdateAppNum != 0)
                updateNotification(needUpdateAppNum)
            else
                cancelNotification()
        }
    }

    fun startUpdateNotification(): Pair<Int, Notification> {
        NotificationManagerCompat.from(context).apply {
            builder.setContentTitle("UpgradeAll 更新服务运行中")
                    .setContentText(null)
                    .setProgress(0, 0, false)
                    // TODO: 实现完整的后台更新后应修改为 false，使应用常驻
                    .setOngoing(false)
        }
        return notificationNotify()
    }

    private fun updateStatusNotification(allAppsNum: Int, finishedAppNum: Int) {
        val progress = (finishedAppNum.toDouble() / allAppsNum * 100).toInt()
        NotificationManagerCompat.from(context).apply {
            builder.setContentTitle("检查更新中")
                    .setContentText("已完成: ${finishedAppNum}/${allAppsNum}")
                    .setProgress(100, progress, false)
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

        if (!MiscellaneousUtils.isBackground()) {
            NotificationManagerCompat.from(context).apply {
                builder.run {
                    setContentTitle("$needUpdateAppNum 个应用需要更新")
                    setProgress(0, 0, false)
                    setOngoing(false)
                    setContentText("点按打开应用主页")
                    setContentIntent(resultPendingIntent)
                }
            }
            notificationNotify()
        } else cancelNotification()
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

    private fun notificationNotify(): Pair<Int, Notification> {
        val notification = builder.build()
        NotificationManagerCompat.from(context).notify(updateNotificationId, notification)
        return Pair(updateNotificationId, notification)
    }

    private fun cancelNotification() {
        NotificationManagerCompat.from(context).cancel(updateNotificationId)
    }

}

class UpdateServiceReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        GlobalScope.launch {
            UpdateService.startService(context)
        }
    }

    companion object {
        private val ACTION_SNOOZE = "${context.packageName}.UPDATE_SERVICE_BROADCAST"
        fun setAlarms(t_h: Int) {
            val alarmTime: Long = t_h.toLong() * 60 * 60 * 1000
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
