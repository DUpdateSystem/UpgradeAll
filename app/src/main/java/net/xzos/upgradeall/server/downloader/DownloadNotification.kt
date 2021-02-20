package net.xzos.upgradeall.server.downloader

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.tonyodev.fetch2.Download
import net.xzos.upgradeall.R
import net.xzos.upgradeall.application.MyApplication
import net.xzos.upgradeall.core.downloader.DownloadOb
import net.xzos.upgradeall.core.filetasker.FileTasker
import net.xzos.upgradeall.core.utils.coroutines.CoroutinesCount
import java.io.File

class DownloadNotification(private val fileTasker: FileTasker) {

    lateinit var taskName: String
    private val notificationIndex: Int = getNotificationIndex()

    private val builder = NotificationCompat.Builder(context, DOWNLOAD_CHANNEL_ID).apply {
        priority = NotificationCompat.PRIORITY_LOW
    }

    fun getDownloadOb() = DownloadOb({ taskStart(it) }, { taskRunning(it) }, { taskStop() }, { taskComplete(it) }, { taskCancel() }, { taskFail() })

    init {
        createNotificationChannel()
    }

    internal fun waitDownloadTaskNotification(taskName: String) {
        builder.clearActions()
                .setOngoing(true)
                .setContentTitle("应用下载 $taskName")
                .setContentText("正在准备")
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setProgress(0, PROGRESS_MAX, true)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "取消",
                        getSnoozePendingIntent(DownloadBroadcastReceiver.DOWNLOAD_CANCEL))
        notificationNotify()
    }


    internal fun showInstallNotification(apkFileName: String) {
        builder.clearActions().run {
            setContentTitle(context.getString(R.string.installing) + " " + apkFileName)
            setSmallIcon(android.R.drawable.stat_sys_download_done)
            setProgress(0, 0, false)
            setOngoing(false)
        }
        // TODO: 更全面的安装过程检测
        // 安装失败后回退操作
        notificationNotify()
    }

    private fun taskStart(task: Download) {
        val progressCurrent: Int = task.progress
        val speed = getSpeedText(task)
        builder.clearActions()
                .setContentTitle("应用下载: $taskName")
                .setContentText(speed)
                .setProgress(PROGRESS_MAX, progressCurrent, false)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .addAction(android.R.drawable.ic_media_pause, "暂停",
                        getSnoozePendingIntent(DownloadBroadcastReceiver.DOWNLOAD_PAUSE))
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "取消",
                        getSnoozePendingIntent(DownloadBroadcastReceiver.DOWNLOAD_CANCEL))
        notificationNotify()
    }

    private fun taskRunning(task: Download) {
        val progressCurrent: Int = task.progress
        val speed = getSpeedText(task)
        builder.clearActions()
                .setContentTitle("应用下载: $taskName")
                .setContentText(speed)
                .setProgress(PROGRESS_MAX, progressCurrent, false)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .addAction(android.R.drawable.ic_media_pause, "暂停",
                        getSnoozePendingIntent(DownloadBroadcastReceiver.DOWNLOAD_PAUSE))
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "取消",
                        getSnoozePendingIntent(DownloadBroadcastReceiver.DOWNLOAD_CANCEL))
        notificationNotify()
    }

    private fun taskStop() {
        builder.clearActions()
                .setContentText("下载已暂停")
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .addAction(android.R.drawable.ic_media_pause, "继续",
                        getSnoozePendingIntent(DownloadBroadcastReceiver.DOWNLOAD_CONTINUE))
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "取消",
                        getSnoozePendingIntent(DownloadBroadcastReceiver.DOWNLOAD_CANCEL))
                .setProgress(0, 0, false)
        notificationNotify()
    }

    private fun taskCancel() {
        cancelNotification()
    }

    private fun taskFail() {
        val delTaskSnoozePendingIntent = getSnoozePendingIntent(DownloadBroadcastReceiver.DOWNLOAD_CANCEL)
        builder.clearActions()
                .setContentText("下载失败，点击重试")
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setProgress(0, 0, false)
                .setContentIntent(getSnoozePendingIntent(DownloadBroadcastReceiver.DOWNLOAD_RETRY))
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "取消", delTaskSnoozePendingIntent)
                .setDeleteIntent(delTaskSnoozePendingIntent)
                .setOngoing(false)
        notificationNotify()
    }

    private fun taskComplete(task: Download) {
        val file = File(task.file)
        showManualMenuNotification(file)
    }

    private fun showManualMenuNotification(file: File) {
        builder.clearActions().run {
            setContentTitle("下载完成: $taskName")
            val contentText = "文件路径: ${file.path}"
            setContentText(contentText)
            setStyle(NotificationCompat.BigTextStyle()
                    .bigText(contentText))
            setSmallIcon(android.R.drawable.stat_sys_download_done)
            setProgress(0, 0, false)
            if (fileTasker.installable) {
                addAction(R.drawable.ic_check_mark_circle, "安装 APK 文件",
                        getSnoozePendingIntent(DownloadBroadcastReceiver.INSTALL_APK))
            }
            addAction(android.R.drawable.stat_sys_download_done, "另存文件",
                    getSnoozePendingIntent(DownloadBroadcastReceiver.SAVE_FILE))
            val delTaskSnoozePendingIntent = getSnoozePendingIntent(DownloadBroadcastReceiver.DOWNLOAD_CANCEL)
            addAction(android.R.drawable.ic_menu_delete, "删除", delTaskSnoozePendingIntent)
            setDeleteIntent(delTaskSnoozePendingIntent)
            setOngoing(false)
        }
        notificationNotify()
    }

    private fun notificationNotify() {
        notificationNotify(notificationIndex, builder.build())
    }

    fun cancelNotification() {
        NotificationManagerCompat.from(context).cancel(notificationIndex)
    }

    private fun getSnoozeIntent(extraIdentifierDownloadControlId: Int): Intent {
        return Intent(context, DownloadBroadcastReceiver::class.java).apply {
            action = DownloadBroadcastReceiver.ACTION_SNOOZE
            putExtra(DownloadBroadcastReceiver.EXTRA_IDENTIFIER_FILE_TASKER_ID, fileTasker.id)
            putExtra(DownloadBroadcastReceiver.EXTRA_IDENTIFIER_FILE_TASKER_CONTROL, extraIdentifierDownloadControlId)
            putExtra(DownloadBroadcastReceiver.EXTRA_IDENTIFIER_NOTIFICATION_ID, notificationIndex)
        }
    }

    private fun getSnoozePendingIntent(extraIdentifierDownloadControlId: Int): PendingIntent {
        val snoozeIntent = getSnoozeIntent(extraIdentifierDownloadControlId)
        val flags =
                if (extraIdentifierDownloadControlId == DownloadBroadcastReceiver.INSTALL_APK ||
                        extraIdentifierDownloadControlId == DownloadBroadcastReceiver.SAVE_FILE)
                // 保存文件/安装按钮可多次点击
                    0
                else PendingIntent.FLAG_ONE_SHOT
        return PendingIntent.getBroadcast(context, getPendingIntentIndex(), snoozeIntent, flags)
    }

    private fun getSpeedText(task: Download): String {
        val speed = task.downloadedBytesPerSecond
        return when {
            speed == -1L -> "0 b/s"
            speed < 1024L -> "$speed b/s"
            1024L <= speed && speed < 1024 * 1024L -> "${speed / 1024} kb/s"
            1024 * 1024L <= speed -> "${speed / (1024 * 1024)} mb/s"
            else -> ""
        }
    }

    companion object {
        private const val DOWNLOAD_CHANNEL_ID = "DownloadNotification"
        private const val PROGRESS_MAX = 100
        private val context get() = MyApplication.context
        private var initNotificationChannel = false

        private const val DOWNLOAD_SERVICE_NOTIFICATION_INDEX = 200
        private val NOTIFICATION_INDEX = CoroutinesCount(201)
        private fun getNotificationIndex(): Int = NOTIFICATION_INDEX.up()

        private val PENDING_INTENT_INDEX = CoroutinesCount(0)
        private fun getPendingIntentIndex(): Int = PENDING_INTENT_INDEX.up()

        private val downloadServiceNotificationBuilder
            get() = NotificationCompat.Builder(context, DOWNLOAD_CHANNEL_ID)
                    .setContentTitle("下载服务运行中").setSmallIcon(android.R.drawable.stat_sys_download_done)
                    .apply { priority = NotificationCompat.PRIORITY_LOW }

        val downloadServiceNotificationMaker = fun(): Pair<Int, Notification> {
            return getDownloadServiceNotification()
        }

        private fun getDownloadServiceNotification(): Pair<Int, Notification> {
            return Pair(DOWNLOAD_SERVICE_NOTIFICATION_INDEX,
                    notificationNotify(DOWNLOAD_SERVICE_NOTIFICATION_INDEX, downloadServiceNotificationBuilder.build()))
        }

        private fun notificationNotify(notificationId: Int, notification: Notification): Notification {
            if (!initNotificationChannel) {
                createNotificationChannel()
                initNotificationChannel = true
            }
            NotificationManagerCompat.from(context).notify(notificationId, notification)
            return notification
        }

        fun createNotificationChannel() {
            val notificationManager = context.getSystemService(
                    Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                    && notificationManager.getNotificationChannel(DOWNLOAD_CHANNEL_ID) == null) {
                val channel = NotificationChannel(DOWNLOAD_CHANNEL_ID, "应用下载", NotificationManager.IMPORTANCE_LOW)
                channel.description = "显示更新文件的下载状态"
                channel.enableLights(true)
                channel.enableVibration(false)
                channel.setShowBadge(false)
                notificationManager.createNotificationChannel(channel)
            }
        }
    }
}
