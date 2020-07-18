package net.xzos.upgradeall.server.update

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.xzos.upgradeall.core.server_manager.UpdateManager
import net.xzos.upgradeall.server.update.UpdateNotification.UPDATE_SERVER_RUNNING_NOTIFICATION_ID

class UpdateService : Service() {

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = UpdateNotification.startUpdateNotification(UPDATE_SERVER_RUNNING_NOTIFICATION_ID)
        startForeground(UPDATE_SERVER_RUNNING_NOTIFICATION_ID, notification)
        GlobalScope.launch {
            UpdateManager.renewAll()
            stopSelf(startId)
        }
        return super.onStartCommand(intent, flags, startId)
    }

    companion object {

        fun startService(context: Context) {
            val intent = Intent(context, UpdateService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                context.startForegroundService(intent)
            else context.startService(intent)
        }
    }
}
