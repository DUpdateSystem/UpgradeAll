package net.xzos.upgradeAll.utils

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.arialyy.annotations.Download
import com.arialyy.aria.core.Aria
import com.arialyy.aria.core.download.DownloadEntity
import com.arialyy.aria.core.download.DownloadTask
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.xzos.upgradeAll.R
import net.xzos.upgradeAll.application.MyApplication
import java.io.File


class AriaDownloader(private val isDebug: Boolean) {

    init {
        notificationIndex++
    }

    private val notificationId = notificationIndex
    private lateinit var url: String
    private lateinit var builder: NotificationCompat.Builder

    fun start(fileName: String, URL: String, headers: Map<String, String> = mapOf()): File {
        this.url = URL
        val (isCreated, file) = startDownloadTask(fileName, URL, headers)
        if (isCreated) {
            Aria.download(this).register()
            GlobalScope.launch(Dispatchers.Main) {
                Toast.makeText(context, "${file.name} 任务已添加", Toast.LENGTH_SHORT).show()
            }
            startDownloadNotification(file)
        } else
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "重复任务，忽略", Toast.LENGTH_SHORT).show()
            }
        return file
    }

    @SuppressLint("CheckResult")
    private fun startDownloadTask(fileName: String, URL: String, headers: Map<String, String>): Pair<Boolean, File> {
        // 检查冲突任务
        val taskList = Aria.download(this).totalTaskList
        val taskFileList = mutableListOf<File>()
        // 检查重复任务
        if (Aria.download(this).taskExists(URL)) {
            if (!isDebug) {
                // 继续 并返回已有任务文件
                context.sendBroadcast(getSnoozeIntent(DOWNLOAD_CONTINUE, path = URL, notificationId = 0))
                val filePath = Aria.download(this).getDownloadEntity(URL).filePath
                return Pair(false, File(filePath))
            } else {
                context.sendBroadcast(getSnoozeIntent(DOWNLOAD_CANCEL, path = URL, notificationId = 0))
                GlobalScope.launch(Dispatchers.Main) { Toast.makeText(context, "尝试终止旧的重复任务", Toast.LENGTH_SHORT).show() }
                Thread.sleep(blockingTime)
            }
        }
        for (task in taskList) {
            // 检查重复下载文件
            task as DownloadEntity
            taskFileList.add(File(task.filePath))
        }
        var file = File(context.externalCacheDir, fileName)
        file = FileUtil.renameSameFile(file, taskFileList)
        val downloadTarget = Aria.download(this)
                .load(URL)
                .useServerFileName(true)
                .setFilePath(file.path)
        downloadTarget.addHeaders(headers)
        downloadTarget.start()
        return Pair(true, file)
    }

    private fun startDownloadNotification(file: File) {
        createNotificationChannel()
        builder = NotificationCompat.Builder(context, CHANNEL_ID).apply {
            setContentTitle("应用下载: ${file.name}")
            setContentText("正在准备")
            setSmallIcon(android.R.drawable.stat_sys_download)
            addAction(android.R.drawable.ic_menu_close_clear_cancel, "取消",
                    getSnoozePendingIntent(DOWNLOAD_CANCEL))
            setOngoing(true)
            priority = NotificationCompat.PRIORITY_LOW

        }
        NotificationManagerCompat.from(context).apply {
            notify(notificationId, builder.build())
        }
    }

    @Download.onTaskStart
    fun taskStart(task: DownloadTask?) {
        if (::builder.isInitialized && task != null && task.key == url) {
            val progressCurrent: Int = task.percent
            val speed = task.convertSpeed
            NotificationManagerCompat.from(context).apply {
                builder.mActions.clear()
                builder.setContentTitle("应用下载: ${File(task.filePath).name}")
                        .setContentText(speed)
                        .setProgress(PROGRESS_MAX, progressCurrent, false)
                        .setSmallIcon(android.R.drawable.stat_sys_download)
                        .addAction(android.R.drawable.ic_media_pause, "暂停",
                                getSnoozePendingIntent(DOWNLOAD_PAUSE))
                        .addAction(android.R.drawable.ic_menu_close_clear_cancel, "取消",
                                getSnoozePendingIntent(DOWNLOAD_CANCEL))
                notify(notificationId, builder.build())
            }
        }
    }

    @Download.onTaskResume
    @Download.onTaskRunning
    fun downloadRunningNotification(task: DownloadTask?) {
        if (::builder.isInitialized && task != null && task.key == url) {
            val progressCurrent: Int = task.percent
            val speed = task.convertSpeed
            NotificationManagerCompat.from(context).apply {
                builder.setContentTitle("应用下载: ${File(task.filePath).name}")
                        .setContentText(speed)
                        .setProgress(PROGRESS_MAX, progressCurrent, false)
                        .setSmallIcon(android.R.drawable.stat_sys_download)
                notify(notificationId, builder.build())
            }
        }
    }

    @Download.onTaskStop
    fun taskStop(task: DownloadTask?) {
        if (::builder.isInitialized && task != null && task.key == url) {
            NotificationManagerCompat.from(context).apply {
                builder.mActions.clear()
                builder.setContentText("下载已暂停")
                        .setSmallIcon(android.R.drawable.stat_sys_download_done)
                        .addAction(android.R.drawable.ic_media_pause, "继续",
                                getSnoozePendingIntent(DOWNLOAD_CONTINUE))
                        .addAction(android.R.drawable.ic_menu_close_clear_cancel, "取消",
                                getSnoozePendingIntent(DOWNLOAD_CANCEL))
                        .setProgress(0, 0, false)
                notify(notificationId, builder.build())
            }
        }
    }

    @Download.onTaskFail
    fun taskFail(task: DownloadTask?) {
        if (::builder.isInitialized && task != null && task.key == url) {
            NotificationManagerCompat.from(context).apply {
                builder.mActions.clear()
                builder.setContentText("下载失败，点击重试")
                        .setSmallIcon(android.R.drawable.stat_sys_download_done)
                        .setProgress(0, 0, false)
                        .setContentIntent(getSnoozePendingIntent(DOWNLOAD_RETRY))
                        .addAction(android.R.drawable.ic_menu_close_clear_cancel, "取消",
                                getSnoozePendingIntent(DOWNLOAD_CANCEL))
                        .setDeleteIntent(getSnoozePendingIntent(DOWNLOAD_CANCEL))
                        .setOngoing(false)
                notify(notificationId, builder.build())
            }
        }
    }

    @Download.onTaskComplete
    fun taskFinish(task: DownloadTask?) {
        if (task != null && task.key == url) {
            val file = File(task.filePath)
            val contentText = "文件路径: ${task.filePath}"
            NotificationManagerCompat.from(context).apply {
                builder = NotificationCompat.Builder(context, CHANNEL_ID).apply {
                    setContentTitle("下载完成: ${file.name}")
                    setContentText(contentText)
                    setStyle(NotificationCompat.BigTextStyle()
                            .bigText(contentText))
                    setSmallIcon(android.R.drawable.stat_sys_download_done)
                    setProgress(0, 0, false)
                    mActions.clear()
                    if (ApkInstaller(context).isApkFile(file)) {
                        addAction(R.drawable.ic_check_latest, "点击安装 APK 文件",
                                getSnoozePendingIntent(INSTALL_APK, path = file.path))
                    }
                    addAction(android.R.drawable.ic_menu_delete, "删除",
                            getSnoozePendingIntent(DEL_FILE, path = file.path))
                    setDeleteIntent(getSnoozePendingIntent(DEL_FILE, path = file.path))
                    setOngoing(false)
                    priority = NotificationCompat.PRIORITY_LOW
                }
                notify(notificationId, builder.build())
            }
            delTaskAndUnRegister()
        }
    }

    @Download.onTaskCancel
    fun taskCancel(task: DownloadTask?) {
        if (task != null && task.key == url) {
            cancelNotification(notificationId)
            delTaskAndUnRegister()
        }
    }


    private fun delTaskAndUnRegister() {
        Aria.download(this).load(url).cancel(false)
        Aria.download(this).unRegister()
    }

    private fun getSnoozePendingIntent(extraIdentifierDownloadControl: Int, path: String = url): PendingIntent {
        val snoozeIntent = getSnoozeIntent(extraIdentifierDownloadControl, path, notificationId)
        val flags =
                if (extraIdentifierDownloadControl == INSTALL_APK)
                // 安装应用按钮可多次点击
                    0
                else
                    PendingIntent.FLAG_ONE_SHOT
        return PendingIntent.getBroadcast(context, pendingIntentIndex.toInt(), snoozeIntent, flags)
    }

    private fun getSnoozeIntent(extraIdentifierDownloadControl: Int, path: String, notificationId: Int): Intent {
        return Intent(context, DownloadBroadcastReceiver::class.java).apply {
            action = ACTION_SNOOZE
            putExtra(NOTIFICATION_ID, notificationId)
            putExtra(EXTRA_IDENTIFIER_URL, path)
            putExtra(EXTRA_IDENTIFIER_DOWNLOAD_CONTROL, extraIdentifierDownloadControl)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "应用下载", NotificationManager.IMPORTANCE_LOW)
            channel.description = "显示更新文件的下载状态"
            channel.enableLights(true)
            channel.enableVibration(false)
            channel.setShowBadge(false)
            val notificationManager = context.getSystemService(
                    NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        private val context = MyApplication.context
        private var pendingIntentIndex: Long = 0
            get() {
                field++
                return field
            }
        private const val CHANNEL_ID = "DownloadNotification"
        private const val PROGRESS_MAX = 100
        private var notificationIndex = 200
        private var ACTION_SNOOZE = "${context.packageName}.DOWNLOAD_BROADCAST"
        private const val EXTRA_IDENTIFIER_URL = "DOWNLOAD_URL"
        private const val EXTRA_IDENTIFIER_DOWNLOAD_CONTROL = "DOWNLOAD_CONTROL"
        private const val NOTIFICATION_ID = "NOTIFICATION_ID "
        private const val DOWNLOAD_CANCEL = 1
        private const val DOWNLOAD_RETRY = 2
        private const val DOWNLOAD_PAUSE = 3
        private const val DOWNLOAD_CONTINUE = 4
        private const val INSTALL_APK = 11
        private const val DEL_FILE = 12

        internal const val blockingTime: Long = 100

        private fun cancelNotification(notificationId: Int) {
            NotificationManagerCompat.from(context).cancel(notificationId)
        }
    }

    class DownloadBroadcastReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val url = intent.getStringExtra(EXTRA_IDENTIFIER_URL)
            val notificationId = intent.getIntExtra(NOTIFICATION_ID, 0)
            if (url != null) {
                when (intent.getIntExtra(EXTRA_IDENTIFIER_DOWNLOAD_CONTROL, -1)) {
                    DOWNLOAD_CANCEL -> cancelTest(url, notificationId)
                    DOWNLOAD_RETRY -> Aria.download(this).load(url).reTry()
                    DOWNLOAD_PAUSE -> Aria.download(this).load(url).stop()
                    DOWNLOAD_CONTINUE -> Aria.download(this).load(url).resume()
                    INSTALL_APK -> {
                        val file = File(url)
                        ApkInstaller(context).installApplication(file)
                    }
                    DEL_FILE -> delFile(url, notificationId)
                    // TODO: 安装并删除功能集成
                }
            }
        }

        private fun cancelTest(url: String, notificationId: Int) {
            Aria.download(this).load(url).stop()
            Aria.download(this).load(url).cancel(true)
            cancelNotification(notificationId)
        }

        private fun delFile(url: String, notificationId: Int) {
            cancelNotification(notificationId)
            val file = File(url)
            file.delete()
        }
    }
}
