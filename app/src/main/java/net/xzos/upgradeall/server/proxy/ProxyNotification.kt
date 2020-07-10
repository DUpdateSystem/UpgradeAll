package net.xzos.upgradeall.server.proxy

import android.app.Notification
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import net.xzos.upgradeall.R
import net.xzos.upgradeall.application.MyApplication

object ProxyNotification {
    private val context = MyApplication.context
    private const val CHANNEL_ID = "UpdateServiceNotification"
    val PROXY_SERVER_NOTIFICATION_ID = context.resources.getInteger(R.integer.proxy_server_notification_id)

    private val builder = NotificationCompat.Builder(context, CHANNEL_ID).apply {
        setContentTitle("UpgradeAll 客户端代理服务运行中")
        setOngoing(true)
        setSmallIcon(R.drawable.ic_launcher_foreground)
        priority = NotificationCompat.PRIORITY_LOW
    }

    fun startNotification(): Notification {
        val notification = builder.build()
        NotificationManagerCompat.from(context).notify(PROXY_SERVER_NOTIFICATION_ID, notification)
        return notification
    }
}
