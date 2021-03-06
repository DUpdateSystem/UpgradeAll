package net.xzos.upgradeall.server.update

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import kotlinx.coroutines.runBlocking
import net.xzos.upgradeall.R
import net.xzos.upgradeall.application.MyApplication
import net.xzos.upgradeall.core.server_manager.UpdateManager
import net.xzos.upgradeall.core.server_manager.module.BaseApp
import net.xzos.upgradeall.core.server_manager.module.app.App
import net.xzos.upgradeall.core.server_manager.module.applications.Applications
import net.xzos.upgradeall.ui.activity.MainActivity
import net.xzos.upgradeall.utils.MiscellaneousUtils

class UpdateNotification {
    init {
        createNotificationChannel()
        UpdateManager.observeForever<Unit>(UpdateManager.UPDATE_STATUS_CHANGED, fun(_) {
            if (UpdateManager.isRunning)
                updateStatusNotify()
        })
        UpdateManager.observeForever<Unit>(UpdateManager.UPDATE_STATUS_COMPLETE, fun(_) {
            finishedNotify()
        })
    }

    private fun updateStatusNotify() {
        val allAppsNum = UpdateManager.getAppNum()
        val finishedAppNum = UpdateManager.finishedUpdateAppNum
        updateStatusNotification(allAppsNum, finishedAppNum)
    }

    private fun finishedNotify() {
        val needUpdateAppList = runBlocking { UpdateManager.getNeedUpdateAppList(block = false) }
        if (needUpdateAppList.isNotEmpty())
            updateNotification(needUpdateAppList)
        else
            cancelNotification()
    }

    fun startUpdateNotification(notificationId: Int): Notification {
        builder.setContentTitle("UpgradeAll 更新服务运行中")
                .setContentText(null)
                .setProgress(0, 0, false)
                .setContentIntent(mainActivityPendingIntent)
        return notificationNotify(notificationId)
    }

    private fun updateStatusNotification(allAppsNum: Int, finishedAppNum: Int) {
        val progress = (finishedAppNum.toDouble() / allAppsNum * 100).toInt()
        builder.setContentTitle("检查更新中")
                .setContentText("已完成: ${finishedAppNum}/${allAppsNum}")
                .setProgress(100, progress, false)
                .setOngoing(true)
        notificationNotify(UPDATE_SERVER_RUNNING_NOTIFICATION_ID)
    }

    private fun updateNotification(needUpdateAppList: Set<BaseApp>) {
        val needUpdateApplicationList = needUpdateAppList.filterIsInstance<Applications>()
        var needUpdateAppNum = needUpdateAppList.filterIsInstance<App>().size
        for (applications in needUpdateApplicationList) {
            needUpdateAppNum += runBlocking { applications.needUpdateAppList }.size
        }
        var text = "$needUpdateAppNum 个应用需要更新"
        if (needUpdateApplicationList.isNotEmpty()) {
            text += "（ ${needUpdateApplicationList.size} 个应用市场）"
        }
        if (!MiscellaneousUtils.isBackground()) {
            builder.run {
                setContentTitle(text)
                setProgress(0, 0, false)
                setOngoing(false)
                setContentText("点按打开应用主页")
                setContentIntent(mainActivityPendingIntent)
            }
            notificationNotify(UPDATE_NOTIFICATION_ID, builder.setAutoCancel(true).build())
        }
    }

    private fun createNotificationChannel() {
        val notificationManager = context.getSystemService(
                Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && notificationManager.getNotificationChannel(UPDATE_SERVICE_CHANNEL_ID) == null) {
            val channel = NotificationChannel(UPDATE_SERVICE_CHANNEL_ID, "更新服务", NotificationManager.IMPORTANCE_MIN)
            channel.description = "显示更新服务状态"
            channel.enableLights(false)
            channel.enableVibration(false)
            channel.setShowBadge(true)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun notificationNotify(notificationId: Int, notification: Notification): Notification {
        NotificationManagerCompat.from(context).notify(notificationId, notification)
        return notification
    }

    private fun notificationNotify(notificationId: Int): Notification {
        val notification = builder.build()
        return notificationNotify(notificationId, notification)
    }

    private fun cancelNotification() {
        NotificationManagerCompat.from(context).cancel(UPDATE_SERVER_RUNNING_NOTIFICATION_ID)
    }

    companion object {
        private val context = MyApplication.context
        private const val UPDATE_SERVICE_CHANNEL_ID = "UpdateServiceNotification"
        private val UPDATE_NOTIFICATION_ID = context.resources.getInteger(R.integer.update_notification_id)
        val UPDATE_SERVER_RUNNING_NOTIFICATION_ID = context.resources.getInteger(R.integer.update_server_running_notification_id)

        private val mainActivityPendingIntent: PendingIntent? = TaskStackBuilder.create(context).run {
            addNextIntentWithParentStack(Intent(context, MainActivity::class.java))
            getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
        }

        private val builder = NotificationCompat.Builder(context, UPDATE_SERVICE_CHANNEL_ID).apply {
            setContentTitle("UpgradeAll 更新服务运行中")
            setOngoing(true)
            setSmallIcon(R.drawable.ic_launcher_foreground)
            priority = NotificationCompat.PRIORITY_LOW
        }
    }
}
