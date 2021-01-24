package net.xzos.upgradeall.server.update

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import net.xzos.upgradeall.core.manager.AppManager
import net.xzos.upgradeall.core.utils.runWithLock
import net.xzos.upgradeall.server.update.UpdateNotification.Companion.UPDATE_SERVER_RUNNING_NOTIFICATION_ID

class UpdateService : Service() {

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.getBooleanExtra(FOREGROUND, true)?.let {
            if (it && !mutex.isLocked) {
                mutex.runWithLock {
                    val updateNotification = UpdateNotification()
                    val notification = updateNotification.startUpdateNotification(UPDATE_SERVER_RUNNING_NOTIFICATION_ID)
                    startForeground(UPDATE_SERVER_RUNNING_NOTIFICATION_ID, notification)
                    runBlocking {
                        AppManager.renewApp(updateNotification.renewStatusFun)
                        stopSelf(startId)
                    }
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    companion object {

        private const val FOREGROUND = "FOREGROUND"

        private val mutex = Mutex()

        fun startService(context: Context, foreground: Boolean = true) {
            val intent = Intent(context, UpdateService::class.java).also {
                it.putExtra(FOREGROUND, foreground)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && foreground)
                context.startForegroundService(intent)
            else context.startService(intent)
        }
    }
}
