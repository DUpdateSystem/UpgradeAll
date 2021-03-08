package net.xzos.upgradeall.server.downloader

import net.xzos.upgradeall.core.filetasker.FileTasker
import net.xzos.upgradeall.core.utils.coroutines.coroutinesMutableMapOf

object DownloadNotificationManager {
    private val notificationMap = coroutinesMutableMapOf<FileTasker, DownloadNotification>(true)

    private fun cancelAllNotification() {
        notificationMap.forEach {
            it.value.cancelNotification()
        }
    }

    fun addNotification(fileTasker: FileTasker, notification: DownloadNotification) {
        notificationMap[fileTasker]?.apply {
            cancelNotification()
            removeNotification(this)
        }
        notificationMap[fileTasker] = notification
    }

    fun getNotification(fileTasker: FileTasker): DownloadNotification? {
        return notificationMap[fileTasker]
    }

    fun removeNotification(notification: DownloadNotification) {
        notificationMap.forEach {
            if (it.value == notification)
                notificationMap.remove(it.key)
        }
    }
}