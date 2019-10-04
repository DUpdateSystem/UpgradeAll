package net.xzos.upgradeAll.utils

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context.NOTIFICATION_SERVICE
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.arialyy.annotations.Download
import com.arialyy.annotations.Upload
import com.arialyy.aria.core.Aria
import com.arialyy.aria.core.download.DownloadEntity
import com.arialyy.aria.core.download.DownloadTarget
import com.arialyy.aria.core.download.DownloadTask
import net.xzos.upgradeAll.application.MyApplication
import net.xzos.upgradeAll.json.nongson.MyCookieManager
import java.io.File


class AriaDownloader(private val CookieManager: MyCookieManager) {

    init {
        notificationId++
    }

    private lateinit var downloadTarget: DownloadTarget
    private lateinit var builder: NotificationCompat.Builder
    private lateinit var targetName: String

    fun start(fileName: String, URL: String): File {
        Aria.download(this).load(URL).cancel(true)  // 取消已有任务，避免冲突
        val cookies = CookieManager.getCookies(URL)
        var cookieString = ""
        for (key in cookies.keys) {
            cookieString += "$key=${cookies[key]}; "
        }
        cookieString = cookieString.substringBeforeLast(";")
        // 检查冲突任务
        val taskList = Aria.download(this).totalTaskList
        val taskFileList = mutableListOf<File>()
        for (task in taskList) {
            task as DownloadEntity
            taskFileList.add(File(task.filePath))
        }
        var file = File(MyApplication.context.externalCacheDir, fileName)
        file = FileUtil.renameSameFile(file, taskFileList)
        downloadTarget = Aria.download(this)
                .load(URL)
                .useServerFileName(true)
                .setFilePath(file.path)
        @SuppressLint("CheckResult")
        if (cookieString.isNotBlank()) {
            downloadTarget.addHeader("Cookie", cookieString)
        }
        downloadTarget.start()
        targetName = downloadTarget.targetName
        startDownloadNotification(file.name)
        return file
    }

    private fun startDownloadNotification(fileName: String) {
        createNotificationChannel()
        builder = NotificationCompat.Builder(MyApplication.context, CHANNEL_ID).apply {
            setContentTitle("应用下载")
            setContentText(fileName)
            setSmallIcon(android.R.drawable.stat_sys_download)
            priority = NotificationCompat.PRIORITY_LOW
        }
        builder.setOngoing(true)
        NotificationManagerCompat.from(MyApplication.context).apply {
            notify(notificationId, builder.build())
        }
    }

    @Download.onTaskRunning
    fun downloadRunningNotification(task: DownloadTask) {
        Log.e("111", "Finish")
        val progressCurrent: Int = task.percent    //任务进度百分比
        val speed = task.convertSpeed    //转换单位后的下载速度，单位转换需要在配置文件中打开
        NotificationManagerCompat.from(MyApplication.context).apply {
            // Issue the initial notification with zero progress
            builder.setProgress(PROGRESS_MAX, progressCurrent, false)
            notify(notificationId, builder.build())
        }
    }

    @Download.onTaskFail
    @Download.onTaskComplete
    fun taskFinish(task: DownloadTask) {
        Log.e("111", "Finish")
        task.cancel()
        downloadTarget.removeRecord()
        NotificationManagerCompat.from(MyApplication.context).apply {
            builder.setContentText("Download complete")
                    .setProgress(0, 0, false)
                    .setOngoing(false)
            notify(notificationId, builder.build())
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "应用下载", NotificationManager.IMPORTANCE_LOW)
            channel.description = "显示更新文件下载状态"
            channel.enableLights(true)
            channel.enableVibration(false)
            channel.setShowBadge(false)
            val notificationManager = MyApplication.context.getSystemService(
                    NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "DownloadNotification"
        const val PROGRESS_MAX = 100
        var notificationId = 200
    }
}
