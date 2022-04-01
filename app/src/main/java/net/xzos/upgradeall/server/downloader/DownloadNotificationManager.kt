package net.xzos.upgradeall.server.downloader

import net.xzos.upgradeall.core.utils.coroutines.coroutinesMutableMapOf
import net.xzos.upgradeall.wrapper.download.DownloadTasker

object DownloadNotificationManager {
    private val notificationMap = coroutinesMutableMapOf<DownloadTasker, DownloadNotification>(true)

    private fun cancelAllNotification() {
        notificationMap.forEach {
            it.value.cancelNotification()
        }
    }

    fun addNotification(downloadTasker: DownloadTasker, notification: DownloadNotification) {
        notificationMap[downloadTasker]?.apply {
            cancelNotification()
            removeNotification(this)
        }
        notificationMap[downloadTasker] = notification
    }

    fun getNotification(downloadTasker: DownloadTasker): DownloadNotification? {
        return notificationMap[downloadTasker]
    }

    fun removeNotification(notification: DownloadNotification) {
        notificationMap.forEach {
            if (it.value == notification)
                notificationMap.remove(it.key)
        }
    }
}