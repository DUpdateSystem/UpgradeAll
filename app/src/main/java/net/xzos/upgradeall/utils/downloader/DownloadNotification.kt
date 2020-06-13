package net.xzos.upgradeall.utils.downloader

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
import com.arialyy.aria.core.task.DownloadTask
import net.xzos.upgradeall.R
import net.xzos.upgradeall.application.MyApplication
import net.xzos.upgradeall.core.oberver.Observer
import net.xzos.upgradeall.utils.downloader.AriaRegister.getCancelNotifyKey
import net.xzos.upgradeall.utils.downloader.AriaRegister.getCompleteNotifyKey
import net.xzos.upgradeall.utils.downloader.AriaRegister.getFailNotifyKey
import net.xzos.upgradeall.utils.downloader.AriaRegister.getRunningNotifyKey
import net.xzos.upgradeall.utils.downloader.AriaRegister.getStartNotifyKey
import net.xzos.upgradeall.utils.downloader.AriaRegister.getStopNotifyKey
import net.xzos.upgradeall.utils.install.isApkFile
import java.io.File

class DownloadNotification(private val url: String) {

    private val notificationIndex: Int = NOTIFICATION_INDEX

    private val builder = NotificationCompat.Builder(context, CHANNEL_ID).apply {
        priority = NotificationCompat.PRIORITY_LOW
    }

    private val startObserver = object : Observer {
        override fun onChanged(vars: Array<out Any>): Any? {
            return taskStart(vars[0] as DownloadTask)
        }
    }

    private val runningObserver = object : Observer {
        override fun onChanged(vars: Array<out Any>): Any? {
            return taskRunning(vars[0] as DownloadTask)
        }
    }

    private val stopObserver = object : Observer {
        override fun onChanged(vars: Array<out Any>): Any? {
            return taskStop()
        }
    }

    private val completeObserver = object : Observer {
        override fun onChanged(vars: Array<out Any>): Any? {
            return taskComplete(vars[0] as DownloadTask).also {
                unregister()
            }
        }
    }

    private val cancelObserver = object : Observer {
        override fun onChanged(vars: Array<out Any>): Any? {
            return taskCancel().also {
                unregister()
            }
        }
    }

    private val failObserver = object : Observer {
        override fun onChanged(vars: Array<out Any>): Any? {
            return taskFail()
        }
    }

    init {
        createNotificationChannel()
    }

    fun finalize() {
        unregister()
    }

    fun register() {
        AriaRegister.observeForever(url.getStartNotifyKey(), startObserver)
        AriaRegister.observeForever(url.getRunningNotifyKey(), runningObserver)
        AriaRegister.observeForever(url.getStopNotifyKey(), stopObserver)
        AriaRegister.observeForever(url.getCompleteNotifyKey(), completeObserver)
        AriaRegister.observeForever(url.getCancelNotifyKey(), cancelObserver)
        AriaRegister.observeForever(url.getFailNotifyKey(), failObserver)
    }

    private fun unregister() {
        AriaRegister.removeObserver(startObserver)
        AriaRegister.removeObserver(runningObserver)
        AriaRegister.removeObserver(stopObserver)
        AriaRegister.removeObserver(completeObserver)
        AriaRegister.removeObserver(cancelObserver)
        AriaRegister.removeObserver(failObserver)
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

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "应用下载", NotificationManager.IMPORTANCE_LOW)
            channel.description = "显示更新文件的下载状态"
            channel.enableLights(true)
            channel.enableVibration(false)
            channel.setShowBadge(false)
            val notificationManager = context.getSystemService(
                    Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
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

    fun taskStart(task: DownloadTask) {
        val progressCurrent: Int = task.percent
        val speed = task.convertSpeed
        builder.clearActions()
                .setContentTitle("应用下载: ${File(task.filePath).name}")
                .setContentText(speed)
                .setProgress(PROGRESS_MAX, progressCurrent, false)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .addAction(android.R.drawable.ic_media_pause, "暂停",
                        getSnoozePendingIntent(DownloadBroadcastReceiver.DOWNLOAD_PAUSE))
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "取消",
                        getSnoozePendingIntent(DownloadBroadcastReceiver.DOWNLOAD_CANCEL))
        notificationNotify()
    }

    fun taskRunning(task: DownloadTask) {
        val progressCurrent: Int = task.percent
        val speed = task.convertSpeed
        builder.setContentTitle("应用下载: ${File(task.filePath).name}")
                .setContentText(speed)
                .setProgress(PROGRESS_MAX, progressCurrent, false)
                .setSmallIcon(android.R.drawable.stat_sys_download)
        notificationNotify()
    }

    fun taskStop() {
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

    fun taskFail() {
        val delTaskSnoozePendingIntent = getSnoozePendingIntent(DownloadBroadcastReceiver.DOWNLOAD_CANCEL)
        builder.clearActions()
                .setContentText("下载失败，点击重试")
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setProgress(0, 0, false)
                .setContentIntent(getSnoozePendingIntent(DownloadBroadcastReceiver.DOWNLOAD_RESTART))
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "取消", delTaskSnoozePendingIntent)
                .setDeleteIntent(delTaskSnoozePendingIntent)
                .setOngoing(false)
        notificationNotify()
    }

    fun taskComplete(task: DownloadTask) {
        val file = File(task.filePath)
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
            putExtra(DownloadBroadcastReceiver.EXTRA_IDENTIFIER_DOWNLOADER_URL, url)
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
        private const val CHANNEL_ID = "DownloadNotification"
        private const val PROGRESS_MAX = 100
        private val context = MyApplication.context

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

        private val downloadServiceNotificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("下载服务运行中").setSmallIcon(android.R.drawable.stat_sys_download_done)
                .apply { priority = NotificationCompat.PRIORITY_LOW }

        fun getDownloadServiceNotification(): Pair<Int, Notification> {
            return Pair(DOWNLOAD_SERVICE_NOTIFICATION_INDEX,
                    notificationNotify(DOWNLOAD_SERVICE_NOTIFICATION_INDEX, downloadServiceNotificationBuilder.build()))
        }

        private fun notificationNotify(notificationId: Int, notification: Notification): Notification {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
            return notification
        }
    }
}
