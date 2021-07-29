package net.xzos.upgradeall.server.downloader

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
import com.tonyodev.fetch2.Download
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import net.xzos.upgradeall.R
import net.xzos.upgradeall.application.MyApplication
import net.xzos.upgradeall.core.downloader.DownloadOb
import net.xzos.upgradeall.core.filetasker.FileTasker
import net.xzos.upgradeall.core.utils.FlagDelegate
import net.xzos.upgradeall.core.utils.coroutines.CoroutinesCount
import net.xzos.upgradeall.core.utils.runWithLock
import net.xzos.upgradeall.data.PreferencesMap
import java.io.File

class DownloadNotification(private val fileTasker: FileTasker) {

    private val taskName: String = fileTasker.name
    private val notificationIndex: Int = getNotificationIndex()

    private val builder = NotificationCompat.Builder(context, DOWNLOAD_CHANNEL_ID).apply {
        priority = NotificationCompat.PRIORITY_LOW
    }

    private val renewMutex = Mutex()

    fun getDownloadOb() = DownloadOb(
        { renewMutex.runWithLock { taskRunning(it) } },
        { renewMutex.runWithLock { taskRunning(it) } },
        { renewMutex.runWithLock { taskStop() } },
        { renewMutex.runWithLock { taskComplete(it) } },
        { renewMutex.runWithLock { taskCancel() } },
        { renewMutex.runWithLock { taskFail() } })

    init {
        createNotificationChannel()
        DownloadNotificationManager.addNotification(fileTasker, this)
    }

    fun waitDownloadTaskNotification() {
        builder.clearActions()
            .setOngoing(true)
            .setContentTitle("${getString(R.string.file_download)}: $taskName")
            .setContentText(getString(R.string.waiting_pre_process))
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(0, PROGRESS_MAX, true)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.cancel),
                getSnoozePendingIntent(DownloadBroadcastReceiver.DOWNLOAD_CANCEL)
            )
        notificationNotify()
    }

    fun showInstallNotification(apkFileName: String) {
        builder.clearActions().run {
            setContentTitle("${getString(R.string.installing)}: $apkFileName")
            setSmallIcon(android.R.drawable.stat_sys_download_done)
            setProgress(0, 0, false)
            setDeleteIntent(getSnoozePendingIntent(DownloadBroadcastReceiver.NOTIFY_CANCEL))
            setNotificationCanGoing()
        }
        // TODO: 更全面的安装过程检测
        // 安装失败后回退操作
        notificationNotify()
    }

    private fun taskRunning(task: Download) {
        val progressCurrent: Int = task.progress
        val speed = getSpeedText(task)
        builder.clearActions()
            .setContentTitle("${getString(R.string.file_download)}: $taskName")
            .setContentText(speed)
            .setProgress(PROGRESS_MAX, progressCurrent, false)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .addAction(
                android.R.drawable.ic_media_pause, getString(R.string.pause),
                getSnoozePendingIntent(DownloadBroadcastReceiver.DOWNLOAD_PAUSE)
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.cancel),
                getSnoozePendingIntent(DownloadBroadcastReceiver.DOWNLOAD_CANCEL)
            )
        notificationNotify()
    }

    private fun taskStop() {
        builder.clearActions()
            .setContentText(getString(R.string.download_paused))
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .addAction(
                android.R.drawable.ic_media_pause, getString(R.string.Continue),
                getSnoozePendingIntent(DownloadBroadcastReceiver.DOWNLOAD_CONTINUE)
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.cancel),
                getSnoozePendingIntent(DownloadBroadcastReceiver.DOWNLOAD_CANCEL)
            )
            .setProgress(0, 0, false)
        notificationNotify()
    }

    private fun taskCancel() {
        cancelNotification()
    }

    private fun taskFail() {
        val delTaskSnoozePendingIntent =
            getSnoozePendingIntent(DownloadBroadcastReceiver.DOWNLOAD_CANCEL)
        builder.clearActions()
            .setContentText(getString(R.string.download_failed_click_to_retry))
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setProgress(0, 0, false)
            .setContentIntent(getSnoozePendingIntent(DownloadBroadcastReceiver.DOWNLOAD_RETRY))
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                getString(R.string.cancel),
                delTaskSnoozePendingIntent
            )
            .setDeleteIntent(delTaskSnoozePendingIntent)
        setNotificationCanGoing()
        notificationNotify()
    }

    private fun taskComplete(task: Download) {
        val file = File(task.file)
        showManualMenuNotification(file)
        if (PreferencesMap.auto_install) {
            GlobalScope.launch { installFileTasker(fileTasker, context) }
        }
    }

    private fun showManualMenuNotification(file: File) {
        builder.clearActions().run {
            setContentTitle("${getString(R.string.download_complete)}: $taskName")
            val contentText = "${getString(R.string.file_path)}: ${file.path}"
            setContentText(contentText)
            setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(contentText)
            )
            setSmallIcon(android.R.drawable.stat_sys_download_done)
            setProgress(0, 0, false)
            runBlocking {
                if (fileTasker.isInstallable(context)) {
                    addAction(
                        R.drawable.ic_check_mark_circle, getString(R.string.install),
                        getSnoozePendingIntent(DownloadBroadcastReceiver.INSTALL_APK)
                    )
                }
            }
            addAction(
                android.R.drawable.stat_sys_download_done, getString(R.string.open_file),
                getSnoozePendingIntent(DownloadBroadcastReceiver.OPEN_FILE)
            )
            addAction(
                android.R.drawable.ic_menu_delete,
                getString(R.string.delete),
                getSnoozePendingIntent(DownloadBroadcastReceiver.DOWNLOAD_CANCEL)
            )
            setDeleteIntent(getSnoozePendingIntent(DownloadBroadcastReceiver.NOTIFY_CANCEL))
        }
        setNotificationCanGoing()
        notificationNotify()
    }

    private fun getString(@StringRes resId: Int): CharSequence = context.getString(resId)

    private fun notificationNotify() {
        notificationNotify(notificationIndex, builder.build())
    }

    fun cancelNotification() {
        NotificationManagerCompat.from(context).cancel(notificationIndex)
        DownloadNotificationManager.removeNotification(this)
    }

    private fun getSnoozeIntent(extraIdentifierDownloadControlId: Int): Intent {
        return Intent(context, DownloadBroadcastReceiver::class.java).apply {
            action = DownloadBroadcastReceiver.ACTION_SNOOZE
            putExtra(DownloadBroadcastReceiver.EXTRA_IDENTIFIER_FILE_TASKER_ID, fileTasker.id)
            putExtra(
                DownloadBroadcastReceiver.EXTRA_IDENTIFIER_FILE_TASKER_CONTROL,
                extraIdentifierDownloadControlId
            )
        }
    }

    private fun getSnoozePendingIntent(extraIdentifierDownloadControlId: Int): PendingIntent {
        val snoozeIntent = getSnoozeIntent(extraIdentifierDownloadControlId)
        val flags =
            if (extraIdentifierDownloadControlId == DownloadBroadcastReceiver.INSTALL_APK ||
                extraIdentifierDownloadControlId == DownloadBroadcastReceiver.OPEN_FILE
            )
            // 保存文件/安装按钮可多次点击
                0
            else PendingIntent.FLAG_ONE_SHOT
        return PendingIntent.getBroadcast(
            context,
            getPendingIntentIndex(),
            snoozeIntent,
            flags or FlagDelegate.PENDING_INTENT_FLAG_IMMUTABLE
        )
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

    private fun setNotificationCanGoing() {
        with(builder) {
            setDeleteIntent(getSnoozePendingIntent(DownloadBroadcastReceiver.NOTIFY_CANCEL))
            setOngoing(false)
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
                .setContentTitle(getString(R.string.download_service_running))
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .apply { priority = NotificationCompat.PRIORITY_LOW }

        val downloadServiceNotificationMaker = fun(): Pair<Int, Notification> {
            return getDownloadServiceNotification()
        }

        private fun getDownloadServiceNotification(): Pair<Int, Notification> {
            return Pair(
                DOWNLOAD_SERVICE_NOTIFICATION_INDEX,
                notificationNotify(
                    DOWNLOAD_SERVICE_NOTIFICATION_INDEX,
                    downloadServiceNotificationBuilder.build()
                )
            )
        }

        private fun notificationNotify(
            notificationId: Int,
            notification: Notification
        ): Notification {
            if (!initNotificationChannel) {
                createNotificationChannel()
                initNotificationChannel = true
            }
            NotificationManagerCompat.from(context).notify(notificationId, notification)
            return notification
        }

        fun createNotificationChannel() {
            val notificationManager = context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && notificationManager.getNotificationChannel(DOWNLOAD_CHANNEL_ID) == null
            ) {
                val channel = NotificationChannel(
                    DOWNLOAD_CHANNEL_ID,
                    getString(R.string.file_download),
                    NotificationManager.IMPORTANCE_LOW
                )
                channel.description = getString(R.string.show_download_status)
                channel.enableLights(true)
                channel.enableVibration(false)
                channel.setShowBadge(false)
                notificationManager.createNotificationChannel(channel)
            }
        }

        fun getString(@StringRes res: Int): String = context.getString(res)
    }
}
