package net.xzos.upgradeall.server.downloader

import android.annotation.SuppressLint
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
import net.xzos.upgradeall.core.oberver.ObserverFun
import net.xzos.upgradeall.server.downloader.DownloadRegister.getCancelNotifyKey
import net.xzos.upgradeall.server.downloader.DownloadRegister.getCompleteNotifyKey
import net.xzos.upgradeall.server.downloader.DownloadRegister.getFailNotifyKey
import net.xzos.upgradeall.server.downloader.DownloadRegister.getRunningNotifyKey
import net.xzos.upgradeall.server.downloader.DownloadRegister.getStartNotifyKey
import net.xzos.upgradeall.server.downloader.DownloadRegister.getStopNotifyKey
import net.xzos.upgradeall.utils.install.isApkFile
import java.io.File

class DownloadNotification(private val downloadId: Int) {

    private val notificationIndex: Int = NOTIFICATION_INDEX

    private val builder = NotificationCompat.Builder(context, DOWNLOAD_CHANNEL_ID).apply {
        priority = NotificationCompat.PRIORITY_LOW
    }

    private val startObserverFun: ObserverFun<Download> = fun(downloadTask) {
        taskStart(downloadTask)
    }

    private val runningObserverFun: ObserverFun<Download> = fun(downloadTask) {
        taskRunning(downloadTask)
    }

    private val stopObserverFun: ObserverFun<Download> = fun(_) { taskStop() }

    private val completeObserverFun: ObserverFun<Download> = fun(downloadTask) {
        taskComplete(downloadTask).also { unregister() }
    }

    private val cancelObserverFun: ObserverFun<Download> = fun(_) {
        taskCancel().also { unregister() }
    }

    private val failObserverFun: ObserverFun<Download> = fun(_) { taskFail() }

    init {
        createNotificationChannel()
    }

    fun finalize() {
        unregister()
    }

    fun register() {
        DownloadRegister.observeForever(downloadId.getStartNotifyKey(), startObserverFun)
        DownloadRegister.observeForever(downloadId.getRunningNotifyKey(), runningObserverFun)
        DownloadRegister.observeForever(downloadId.getStopNotifyKey(), stopObserverFun)
        DownloadRegister.observeForever(downloadId.getCompleteNotifyKey(), completeObserverFun)
        DownloadRegister.observeForever(downloadId.getCancelNotifyKey(), cancelObserverFun)
        DownloadRegister.observeForever(downloadId.getFailNotifyKey(), failObserverFun)
    }

    private fun unregister() {
        DownloadRegister.removeObserver(startObserverFun)
        DownloadRegister.removeObserver(runningObserverFun)
        DownloadRegister.removeObserver(stopObserverFun)
        DownloadRegister.removeObserver(completeObserverFun)
        DownloadRegister.removeObserver(cancelObserverFun)
        DownloadRegister.removeObserver(failObserverFun)
    }

    internal fun waitDownloadTaskNotification(fileName: String? = null) {
        var text = "应用下载"
        if (fileName != null) {
            text += "：$fileName"
        }
        builder.clearActions()
                .setOngoing(true)
                .setContentTitle(text)
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

    @SuppressLint("RestrictedApi")  // 修复 mActions 无法操作
    private fun NotificationCompat.Builder.clearActions(): NotificationCompat.Builder {
        return this.apply {
            mActions.clear()
        }
    }

    private fun taskStart(task: Download) {
        val progressCurrent: Int = task.progress
        val speed = getSpeedText(task)
        builder.clearActions()
                .setContentTitle("应用下载: ${File(task.file).name}")
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
                .setContentTitle("应用下载: ${File(task.file).name}")
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

    fun taskCancel() {
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
            setContentTitle("下载完成: ${file.name}")
            val contentText = "文件路径: ${file.path}"
            setContentText(contentText)
            setStyle(NotificationCompat.BigTextStyle()
                    .bigText(contentText))
            setSmallIcon(android.R.drawable.stat_sys_download_done)
            setProgress(0, 0, false)
            if (file.isApkFile()) {
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

    private fun cancelNotification() {
        NotificationManagerCompat.from(context).cancel(notificationIndex)
    }

    private fun getSnoozeIntent(extraIdentifierDownloadControlId: Int): Intent {
        return Intent(context, DownloadBroadcastReceiver::class.java).apply {
            action = DownloadBroadcastReceiver.ACTION_SNOOZE
            putExtra(DownloadBroadcastReceiver.EXTRA_IDENTIFIER_DOWNLOADER_URL, downloadId)
            putExtra(DownloadBroadcastReceiver.EXTRA_IDENTIFIER_DOWNLOAD_CONTROL, extraIdentifierDownloadControlId)
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
        return PendingIntent.getBroadcast(context, PENDING_INTENT_INDEX, snoozeIntent, flags)
    }

    companion object {
        private const val DOWNLOAD_CHANNEL_ID = "DownloadNotification"
        private const val PROGRESS_MAX = 100
        private val context = MyApplication.context
        private var initNotificationChannel = false

        private const val DOWNLOAD_SERVICE_NOTIFICATION_INDEX = 200
        private var NOTIFICATION_INDEX = 201
            get() = field.also {
                field++
            }
        private var PENDING_INTENT_INDEX = 0
            get() {
                field++
                return field
            }

        private val downloadServiceNotificationBuilder = NotificationCompat.Builder(context, DOWNLOAD_CHANNEL_ID)
                .setContentTitle("下载服务运行中").setSmallIcon(android.R.drawable.stat_sys_download_done)
                .apply { priority = NotificationCompat.PRIORITY_LOW }

        fun getDownloadServiceNotification(): Pair<Int, Notification> {
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
}
