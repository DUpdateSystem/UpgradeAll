package net.xzos.upgradeall.utils

import android.app.NotificationManager
import android.content.Context

fun getNotificationManager(context: Context) = context.getSystemService(
    Context.NOTIFICATION_SERVICE
) as NotificationManager

fun cleaNotification(context: Context) {
    getNotificationManager(context).cancelAll()
}
