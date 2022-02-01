package net.xzos.upgradeall.core.installer.service

import android.app.Service
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.IBinder
import net.xzos.upgradeall.core.utils.log.Log
import net.xzos.upgradeall.core.utils.log.ObjectTag
import net.xzos.upgradeall.core.utils.log.ObjectTag.Companion.core
import net.xzos.upgradeall.core.utils.log.msg

class ApkInstallerService : Service() {

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (val flag = if (intent?.hasExtra(PackageInstaller.EXTRA_STATUS) == true)
            intent.getIntExtra(PackageInstaller.EXTRA_STATUS, 0)
        else null) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                Log.d(logObjectTag, TAG, "Requesting user confirmation for installation")
                val confirmationIntent = intent?.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
                confirmationIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try {
                    startActivity(confirmationIntent)
                } catch (e: Exception) {
                    Log.e(logObjectTag, TAG, "Request Activity e: ${e.msg()}")
                }
            }
            PackageInstaller.STATUS_SUCCESS -> Log.d(logObjectTag, TAG, "Installation succeed")
            else -> Log.d(logObjectTag, TAG, "Installation failed, flag: $flag")
        }
        stopSelf()
        return START_NOT_STICKY
    }

    companion object {
        internal const val TAG = "ApkInstallerService"
        internal val logObjectTag = ObjectTag(core, TAG)
    }
}