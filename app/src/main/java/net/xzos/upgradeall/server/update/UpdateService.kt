package net.xzos.upgradeall.server.update

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.xzos.upgradeall.core.server_manager.UpdateManager

/**
 * 启动 UpdateManager#renewAll()
 */
class UpdateService : Service() {

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val (id, notification) = net.xzos.upgradeall.server.update.UpdateManager.startUpdateNotification()
        startForeground(id, notification)
        GlobalScope.launch {
            UpdateManager.renewAll()
            stopSelf()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    companion object {
        fun startService(context: Context) {
            val intent = Intent(context, UpdateService::class.java)
            context.startService(intent)
        }
    }
}
