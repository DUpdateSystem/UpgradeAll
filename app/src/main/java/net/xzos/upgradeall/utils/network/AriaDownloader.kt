package net.xzos.upgradeall.utils.network

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.arialyy.annotations.Download
import com.arialyy.aria.core.Aria
import com.arialyy.aria.core.download.DownloadEntity
import com.arialyy.aria.core.download.DownloadTask
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.xzos.upgradeall.core.data_manager.utils.FilePathUtils
import net.xzos.upgradeall.R
import net.xzos.upgradeall.application.MyApplication
import net.xzos.upgradeall.application.MyApplication.Companion.context
import net.xzos.upgradeall.ui.activity.SaveFileActivity
import net.xzos.upgradeall.utils.ApkInstaller
import net.xzos.upgradeall.utils.FileUtil
import net.xzos.upgradeall.utils.MiscellaneousUtils
import net.xzos.upgradeall.utils.isApkFile
import net.xzos.upgradeall.utils.network.DownloadBroadcastReceiver.Companion.ACTION_SNOOZE
import net.xzos.upgradeall.utils.network.DownloadBroadcastReceiver.Companion.DEL_TASK
import net.xzos.upgradeall.utils.network.DownloadBroadcastReceiver.Companion.DOWNLOAD_CANCEL
import net.xzos.upgradeall.utils.network.DownloadBroadcastReceiver.Companion.DOWNLOAD_CONTINUE
import net.xzos.upgradeall.utils.network.DownloadBroadcastReceiver.Companion.DOWNLOAD_PAUSE
import net.xzos.upgradeall.utils.network.DownloadBroadcastReceiver.Companion.DOWNLOAD_RESTART
import net.xzos.upgradeall.utils.network.DownloadBroadcastReceiver.Companion.EXTRA_IDENTIFIER_DOWNLOADER_ID
import net.xzos.upgradeall.utils.network.DownloadBroadcastReceiver.Companion.EXTRA_IDENTIFIER_DOWNLOAD_CONTROL
import net.xzos.upgradeall.utils.network.DownloadBroadcastReceiver.Companion.INSTALL_APK
import net.xzos.upgradeall.utils.network.DownloadBroadcastReceiver.Companion.SAVE_FILE
import java.io.File


class AriaDownloader(private val debugMode: Boolean, private val url: String) {

    private val downloaderId = notificationIndex
    private lateinit var downloadFile: File
    private lateinit var builder: NotificationCompat.Builder


    init {
        createNotificationChannel()
        downloaderMap[downloaderId] = this
    }

    fun start(fileName: String, headers: Map<String, String> = hashMapOf()): File {
        createDownloadTaskNotification(fileName)
        val (isCreated, file) = startDownloadTask(fileName, headers)
        if (isCreated) {
            Aria.download(this).register()
            val text = file.name + context.getString(R.string.download_task_begin)
            MiscellaneousUtils.showToast(context, text = text)
            startDownloadNotification(file)
        } else {
            cancelNotification(downloaderId)
            MiscellaneousUtils.showToast(context, R.string.repeated_download_task)
        }
        return file
    }

    fun resume() {
        Aria.download(this).load(url).resume()
    }

    fun stop() {
        Aria.download(this).load(url).stop()
    }

    fun restart() {
        Aria.download(this).load(url).reStart()
    }

    fun cancel() {
        cancelNotification(downloaderId)
        delTaskAndUnRegister()
        downloaderMap.remove(downloaderId)
    }

    fun delTask() {
        cancel()
        downloadFile.delete()
    }

    fun install() {
        ApkInstaller(context).installApplication(downloadFile)
    }

    fun saveFile() {
        val mimeType = FileUtil.getMimeTypeByUri(context, Uri.fromFile(downloadFile))
        GlobalScope.launch {
            SaveFileActivity.newInstance(
                    downloadFile.name, downloadFile.readBytes(),
                    mimeType, context
            )
        }
    }

    @SuppressLint("RestrictedApi")  // 修复 mActions 无法操作
    private fun NotificationCompat.Builder.clearActions(): NotificationCompat.Builder {
        return this.apply {
            mActions.clear()
        }
    }

    private fun startDownloadTask(fileName: String, headers: Map<String, String>): Pair<Boolean, File> {
        // 检查冲突任务
        val taskList = Aria.download(this).totalTaskList
        val taskFileList = mutableListOf<File>()
        // 检查重复任务
        if (Aria.download(this).taskExists(url)) {
            if (!debugMode) {
                // 继续 并返回已有任务文件
                context.sendBroadcast(getSnoozeIntent(DOWNLOAD_CONTINUE))
                val filePath = Aria.download(this).getDownloadEntity(url).filePath
                return Pair(false, File(filePath))
            } else {
                context.sendBroadcast(getSnoozeIntent(DOWNLOAD_CANCEL))
                MiscellaneousUtils.showToast(context, R.string.try_kill_repeated_download_task)
                Thread.sleep(blockingTime)
            }
        }
        for (task in taskList) {
            // 检查重复下载文件
            task as DownloadEntity
            taskFileList.add(File(task.filePath))
        }
        downloadFile = FilePathUtils.renameSameFile(
                File(downloadDir, fileName), taskFileList
        )
        Aria.download(this)
                .load(url)
                .useServerFileName(true)
                .setFilePath(downloadFile.path)
                .addHeaders(headers)
                .apply {
                    this.start()
                }
        return Pair(true, downloadFile)
    }

    internal fun createDownloadTaskNotification(fileName: String) {
        builder = NotificationCompat.Builder(context, CHANNEL_ID).apply {
            setContentTitle("应用下载: $fileName")
            setContentText("正在准备")
            setSmallIcon(android.R.drawable.stat_sys_download)
            setProgress(0, PROGRESS_MAX, true)
            setOngoing(true)
            priority = NotificationCompat.PRIORITY_LOW

        }
        notificationNotify()
    }

    private fun startDownloadNotification(file: File) {
        NotificationManagerCompat.from(context).apply {
            builder.clearActions()
                    .setContentTitle("应用下载: ${file.name}")
                    .setContentText("正在准备")
                    .setSmallIcon(android.R.drawable.stat_sys_download)
                    .setProgress(0, PROGRESS_MAX, true)
                    .addAction(android.R.drawable.ic_menu_close_clear_cancel, "取消",
                            getSnoozePendingIntent(DOWNLOAD_CANCEL))
                    .setOngoing(true)
        }
        notificationNotify()
    }

    private fun notificationNotify() {
        NotificationManagerCompat.from(context).notify(downloaderId, builder.build())
    }

    @Download.onTaskStart
    fun taskStart(task: DownloadTask?) {
        if (task?.key == url) {
            val progressCurrent: Int = task.percent
            val speed = task.convertSpeed
            NotificationManagerCompat.from(context).apply {
                builder.clearActions()
                        .setContentTitle("应用下载: ${File(task.filePath).name}")
                        .setContentText(speed)
                        .setProgress(PROGRESS_MAX, progressCurrent, false)
                        .setSmallIcon(android.R.drawable.stat_sys_download)
                        .addAction(android.R.drawable.ic_media_pause, "暂停",
                                getSnoozePendingIntent(DOWNLOAD_PAUSE))
                        .addAction(android.R.drawable.ic_menu_close_clear_cancel, "取消",
                                getSnoozePendingIntent(DOWNLOAD_CANCEL))
            }
            notificationNotify()
        }
    }

    @Download.onTaskResume
    @Download.onTaskRunning
    fun downloadRunningNotification(task: DownloadTask?) {
        if (task?.key == url) {
            val progressCurrent: Int = task.percent
            val speed = task.convertSpeed
            NotificationManagerCompat.from(context).apply {
                builder.setContentTitle("应用下载: ${File(task.filePath).name}")
                        .setContentText(speed)
                        .setProgress(PROGRESS_MAX, progressCurrent, false)
                        .setSmallIcon(android.R.drawable.stat_sys_download)
            }
            notificationNotify()
        }
    }

    @Download.onTaskStop
    fun taskStop(task: DownloadTask?) {
        if (task?.key == url) {
            NotificationManagerCompat.from(context).apply {
                builder.clearActions()
                        .setContentText("下载已暂停")
                        .setSmallIcon(android.R.drawable.stat_sys_download_done)
                        .addAction(android.R.drawable.ic_media_pause, "继续",
                                getSnoozePendingIntent(DOWNLOAD_CONTINUE))
                        .addAction(android.R.drawable.ic_menu_close_clear_cancel, "取消",
                                getSnoozePendingIntent(DOWNLOAD_CANCEL))
                        .setProgress(0, 0, false)
            }
            notificationNotify()
        }
    }

    @Download.onTaskFail
    fun taskFail(task: DownloadTask?) {
        if (task?.key == url) {
            NotificationManagerCompat.from(context).apply {
                builder.clearActions()
                        .setContentText("下载失败，点击重试")
                        .setSmallIcon(android.R.drawable.stat_sys_download_done)
                        .setProgress(0, 0, false)
                        .setContentIntent(getSnoozePendingIntent(DOWNLOAD_RESTART))
                        .addAction(android.R.drawable.ic_menu_close_clear_cancel, "取消",
                                getSnoozePendingIntent(DOWNLOAD_CANCEL))
                        .setDeleteIntent(getSnoozePendingIntent(DOWNLOAD_CANCEL))
                        .setOngoing(false)
            }
            notificationNotify()
        }
    }

    @Download.onTaskComplete
    fun taskFinish(task: DownloadTask?) {
        if (task?.key == url) {
            val apkInstaller = ApkInstaller(context)
            val file = File(task.filePath)
            downloadFile = apkInstaller.autoRenameFile(file)
            val contentText = "文件路径: ${downloadFile.path}"
            NotificationManagerCompat.from(context).apply {
                builder.clearActions().run {
                    setContentTitle("下载完成: ${downloadFile.name}")
                    setContentText(contentText)
                    setStyle(NotificationCompat.BigTextStyle()
                            .bigText(contentText))
                    setSmallIcon(android.R.drawable.stat_sys_download_done)
                    setProgress(0, 0, false)
                    if (downloadFile.isApkFile()) {
                        addAction(R.drawable.ic_check_mark_circle, "安装 APK 文件",
                                getSnoozePendingIntent(INSTALL_APK))
                    }
                    addAction(android.R.drawable.stat_sys_download_done, "另存文件",
                            getSnoozePendingIntent(SAVE_FILE))
                    addAction(android.R.drawable.ic_menu_delete, "删除",
                            getSnoozePendingIntent(DEL_TASK))
                    setDeleteIntent(getSnoozePendingIntent(DEL_TASK))
                    setOngoing(false)
                }
            }
            notificationNotify()
            if (debugMode) cancel()
            else delTaskAndUnRegister()
        }
    }

    @Download.onTaskCancel
    fun taskCancel(task: DownloadTask?) {
        if (task?.key == url) {
            cancel()
        }
    }


    private fun delTaskAndUnRegister() {
        with(Aria.download(this)) {
            this.load(url).cancel(false)
            this.unRegister()
        }
    }

    private fun getSnoozePendingIntent(extraIdentifierDownloadControlId: Int): PendingIntent {
        val snoozeIntent = getSnoozeIntent(extraIdentifierDownloadControlId)
        val flags =
                if (extraIdentifierDownloadControlId == INSTALL_APK ||
                        extraIdentifierDownloadControlId == SAVE_FILE)
                // 保存文件/安装按钮可多次点击
                    0
                else PendingIntent.FLAG_ONE_SHOT
        return PendingIntent.getBroadcast(context, pendingIntentIndex, snoozeIntent, flags)
    }

    private fun getSnoozeIntent(extraIdentifierDownloadControlId: Int): Intent {
        return Intent(context, DownloadBroadcastReceiver::class.java).apply {
            action = ACTION_SNOOZE
            putExtra(EXTRA_IDENTIFIER_DOWNLOADER_ID, downloaderId)
            putExtra(EXTRA_IDENTIFIER_DOWNLOAD_CONTROL, extraIdentifierDownloadControlId)
        }
    }

    private fun cancelNotification(downloadNotificationId: Int) {
        NotificationManagerCompat.from(context).cancel(downloadNotificationId)
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        private val context = MyApplication.context
        private var pendingIntentIndex = 0
            get() {
                field++
                return field
            }
        private var notificationIndex = 200
            get() {
                field++
                return field
            }
        private const val CHANNEL_ID = "DownloadNotification"
        private const val PROGRESS_MAX = 100

        internal const val blockingTime: Long = 100

        private val downloadDir = FileUtil.DOWNLOAD_CACHE_DIR

        private val downloaderMap: HashMap<Int, AriaDownloader> = hashMapOf()
        internal fun getDownload(downloadId: Int): AriaDownloader? = downloaderMap[downloadId]

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
    }
}

class DownloadBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val downloaderId = intent.getIntExtra(EXTRA_IDENTIFIER_DOWNLOADER_ID, -1)
        AriaDownloader.getDownload(downloaderId)?.run {
            when (intent.getIntExtra(EXTRA_IDENTIFIER_DOWNLOAD_CONTROL, -1)) {
                DOWNLOAD_CANCEL -> this.cancel()
                DOWNLOAD_RESTART -> this.restart()
                DOWNLOAD_PAUSE -> this.stop()
                DOWNLOAD_CONTINUE -> this.resume()
                SAVE_FILE -> this.saveFile()
                INSTALL_APK -> this.install()
                DEL_TASK -> this.delTask()

                // TODO: 安装并删除功能集成
            }
        }
    }

    companion object {
        internal val ACTION_SNOOZE = "${context.packageName}.DOWNLOAD_BROADCAST"

        internal const val EXTRA_IDENTIFIER_DOWNLOADER_ID = "DOWNLOADER_ID"

        internal const val EXTRA_IDENTIFIER_DOWNLOAD_CONTROL = "DOWNLOAD_CONTROL"
        internal const val DOWNLOAD_CANCEL = 1
        internal const val DOWNLOAD_RESTART = 2
        internal const val DOWNLOAD_PAUSE = 3
        internal const val DOWNLOAD_CONTINUE = 4
        internal const val SAVE_FILE = 10
        internal const val INSTALL_APK = 11
        internal const val DEL_TASK = 12

    }
}
