package net.xzos.upgradeall.server.update

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import net.xzos.upgradeall.R
import net.xzos.upgradeall.application.MyApplication
import net.xzos.upgradeall.core.androidutils.withImmutableFlag
import net.xzos.upgradeall.core.manager.AppManager
import net.xzos.upgradeall.core.module.AppStatus
import net.xzos.upgradeall.ui.home.MainActivity
import net.xzos.upgradeall.utils.MiscellaneousUtils
import net.xzos.upgradeall.utils.getNotificationManager


@SuppressLint("MissingPermission")
class UpdateNotification {
    init {
        createNotificationChannel()
    }

    val renewStatusFun = fun(renewingAppNum: Int, totalAppNum: Int) {
        updateStatusNotify(renewingAppNum, totalAppNum)
    }

    val recheckStatusFun = fun(renewingAppNum: Int, totalAppNum: Int) {
        recheckStatusNotify(renewingAppNum, totalAppNum)
    }

    val updateDone = {
        finishedNotify()
    }

    private fun updateStatusNotify(renewingAppNum: Int, totalAppNum: Int) {
        val finishedAppNum = totalAppNum - renewingAppNum
        updateStatusNotification(totalAppNum, finishedAppNum)
    }

    private fun recheckStatusNotify(renewingAppNum: Int, totalAppNum: Int) {
        val finishedAppNum = totalAppNum - renewingAppNum
        recheckStatusNotification(totalAppNum, finishedAppNum)
    }

    private fun finishedNotify() {
        val needUpdateAppList = AppManager.getAppList(AppStatus.APP_OUTDATED)
        if (needUpdateAppList.isNotEmpty())
            updateNotification(needUpdateAppList.size)
        else
            cancelNotification()
    }

    fun startUpdateNotification(notificationId: Int): Notification {
        builder.setContentTitle(getString(R.string.update_service_running))
            .setContentText(null)
            .setProgress(0, 0, false)
            .setContentIntent(mainActivityPendingIntent)
        return notificationNotify(notificationId)
    }

    private fun updateStatusNotification(allAppsNum: Int, finishedAppNum: Int) {
        setProgressNotification(builder, finishedAppNum, allAppsNum, R.string.update_running)
    }

    private fun recheckStatusNotification(allAppsNum: Int, finishedAppNum: Int) {
        setProgressNotification(
            builder, finishedAppNum, allAppsNum, R.string.update_recheck_running
        )
    }

    private fun setProgressNotification(
        @Suppress("SameParameterValue") builder: NotificationCompat.Builder,
        doneNum: Int, allNum: Int, @StringRes titleId: Int
    ) {
        val progress = (doneNum.toDouble() / allNum * 100).toInt()
        builder.setContentTitle(getString(titleId))
            .setContentText("${getString(R.string.update_progress)}: ${doneNum}/${allNum}")
            .setProgress(100, progress, false)
            .setOngoing(true)
        notificationNotify(UPDATE_SERVER_RUNNING_NOTIFICATION_ID)
    }

    private fun updateNotification(needUpdateAppNum: Int) {
        val text = String.format(getString(R.string.update_format_app_update_tip), needUpdateAppNum)
        if (!MiscellaneousUtils.isBackground()) {
            builder.run {
                setContentTitle(text)
                setProgress(0, 0, false)
                setOngoing(false)
                setContentText(getString(R.string.click_open_homepage))
                setContentIntent(mainActivityPendingIntent)
            }
            notificationNotify(UPDATE_NOTIFICATION_ID, builder.setAutoCancel(true).build())
        }
    }

    private fun createNotificationChannel() {
        val notificationManager = getNotificationManager(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
            && notificationManager.getNotificationChannel(UPDATE_SERVICE_CHANNEL_ID) == null
        ) {
            val channel = NotificationChannel(
                UPDATE_SERVICE_CHANNEL_ID,
                getString(R.string.update_service),
                NotificationManager.IMPORTANCE_MIN
            )
            channel.description = getString(R.string.update_service_desc)
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
        cancelNotification(UPDATE_SERVER_RUNNING_NOTIFICATION_ID)
    }

    fun cancelNotification(notificationId: Int) {
        NotificationManagerCompat.from(context).cancel(notificationId)
    }

    companion object {
        private val context get() = MyApplication.context
        private const val UPDATE_SERVICE_CHANNEL_ID = "UpdateServiceNotification"
        private val UPDATE_NOTIFICATION_ID =
            context.resources.getInteger(R.integer.update_notification_id)
        val UPDATE_SERVER_RUNNING_NOTIFICATION_ID =
            context.resources.getInteger(R.integer.update_server_running_notification_id)

        private val mainActivityPendingIntent: PendingIntent? =
            TaskStackBuilder.create(context).run {
                addNextIntentWithParentStack(Intent(context, MainActivity::class.java))
                getPendingIntent(
                    0,
                    PendingIntent.FLAG_UPDATE_CURRENT.withImmutableFlag()
                )
            }

        @SuppressLint("StaticFieldLeak")
        private val builder = NotificationCompat.Builder(context, UPDATE_SERVICE_CHANNEL_ID).apply {
            setContentTitle(getString(R.string.update_service_running))
            setOngoing(true)
            setSmallIcon(R.drawable.ic_launcher_main)
            priority = NotificationCompat.PRIORITY_LOW
        }

        private fun getString(@StringRes stringRes: Int): String =
            context.getString(stringRes)
    }
}
