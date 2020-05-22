package net.xzos.upgradeall.server.update

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.xzos.upgradeall.application.MyApplication

class UpdateServiceReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        GlobalScope.launch {
            UpdateService.startService(context)
        }
    }

    companion object {
        private val ACTION_SNOOZE = "${MyApplication.context.packageName}.UPDATE_SERVICE_BROADCAST"
        fun setAlarms(t_h: Int) {
            val alarmTime: Long = t_h.toLong() * 60 * 60 * 1000
            val alarmIntent = PendingIntent.getBroadcast(
                    MyApplication.context, 0,
                    Intent(MyApplication.context, UpdateServiceReceiver::class.java).apply { action = ACTION_SNOOZE },
                    PendingIntent.FLAG_UPDATE_CURRENT
            )
            val alarmManager = (MyApplication.context.getSystemService(Context.ALARM_SERVICE) as AlarmManager)
            alarmManager.setInexactRepeating(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + alarmTime,
                    alarmTime, alarmIntent
            )
        }
    }
}
