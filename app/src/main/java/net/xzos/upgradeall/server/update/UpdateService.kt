package net.xzos.upgradeall.server.update

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.xzos.upgradeall.R
import net.xzos.upgradeall.application.MyApplication.Companion.context
import net.xzos.upgradeall.core.oberver.Observer
import net.xzos.upgradeall.core.server_manager.UpdateManager
import net.xzos.upgradeall.server.update.UpdateNotification.FINISH_UPDATE

class UpdateService : Service() {
    private val observer: Observer

    init {
        UpdateNotification.observeForever(tag = FINISH_UPDATE, observer = object : Observer {
            override fun onChanged(vararg vars: Any): Any? {
                return stopSelf()
            }
        }.also {
            observer = it
        })
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    /**
     * 启动 UpdateManager#renewAll()
     */
    override fun onCreate() {
        super.onCreate()
        val notification = UpdateNotification.startUpdateNotification(UPDATE_SERVER_RUNNING_NOTIFICATION_ID)
        startForeground(UPDATE_SERVER_RUNNING_NOTIFICATION_ID, notification)
        GlobalScope.launch {
            UpdateManager.renewAll()
        }
    }

    override fun onDestroy() {
        UpdateNotification.cancelNotification(UPDATE_SERVER_RUNNING_NOTIFICATION_ID)
        UpdateNotification.removeObserver(observer)
        super.onDestroy()
    }

    companion object {
        private val UPDATE_SERVER_RUNNING_NOTIFICATION_ID = context.resources.getInteger(R.integer.update_server_running_notification_id)

        fun startService(context: Context) {
            val intent = Intent(context, UpdateService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                context.startForegroundService(intent)
            else context.startService(intent)
        }
    }
}
