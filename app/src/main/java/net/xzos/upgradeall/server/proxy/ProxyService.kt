package net.xzos.upgradeall.server.proxy

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.xzos.upgradeall.core.network_api.ClientProxy
import net.xzos.upgradeall.core.network_api.GrpcApi
import net.xzos.upgradeall.core.oberver.Observer
import net.xzos.upgradeall.core.server_manager.UpdateManager
import net.xzos.upgradeall.server.proxy.ProxyNotification.PROXY_SERVER_NOTIFICATION_ID

class ProxyService : Service() {

    init {
        UpdateManager.observeForever(object : Observer {
            override fun onChanged(vararg vars: Any): Any? {
                return ClientProxy.stopClientProxy()
            }
        })
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = ProxyNotification.startNotification()
        startForeground(PROXY_SERVER_NOTIFICATION_ID, notification)
        GlobalScope.launch {
            GrpcApi.newClientProxy()
            stopSelf(startId)
        }
        return super.onStartCommand(intent, flags, startId)
    }

    companion object {

        fun startService(context: Context) {
            val intent = Intent(context, ProxyService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                context.startForegroundService(intent)
            else context.startService(intent)
        }
    }
}
