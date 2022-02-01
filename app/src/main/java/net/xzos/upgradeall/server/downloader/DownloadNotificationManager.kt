package net.xzos.upgradeall.server.downloader

import net.xzos.upgradeall.core.utils.coroutines.coroutinesMutableMapOf

object DownloadNotificationManager {
    private val notificationMap = coroutinesMutableMapOf<String, DownloadNotification>(true)

    private fun cancelAllNotification() {
        notificationMap.forEach {
            it.value.cancelNotification()
        }
    }

    fun addNotification(fileTaskerId: String, notification: DownloadNotification) {
        notificationMap[fileTaskerId]?.apply {
            cancelNotification()
            removeNotification(this)
        }
        notificationMap[fileTaskerId] = notification
    }

    fun getNotification(fileTaskerId: String): DownloadNotification? {
        return notificationMap[fileTaskerId]
    }

    fun removeNotification(notification: DownloadNotification) {
        notificationMap.forEach {
            if (it.value == notification)
                notificationMap.remove(it.key)
        }
    }
}