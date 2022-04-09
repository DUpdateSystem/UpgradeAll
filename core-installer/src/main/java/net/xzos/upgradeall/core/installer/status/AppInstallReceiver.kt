package net.xzos.upgradeall.core.installer.status

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import net.xzos.upgradeall.core.utils.log.Log
import net.xzos.upgradeall.core.utils.log.ObjectTag
import net.xzos.upgradeall.core.utils.log.msg


val appInstallReceiver = AppInstallReceiver()

class AppInstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val manager = context.packageManager
        val packageName = intent.data!!.schemeSpecificPart

        try {
            val info = manager.getPackageInfo(packageName, 0)
            InstallObserver.notifyComplete(info)
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(logObjectTag, TAG, "error: ${e.msg()}")
        }
    }

    fun register(context: Context) {
        val intentFilter = IntentFilter()
        intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED)
        intentFilter.addAction(Intent.ACTION_PACKAGE_REPLACED)
        intentFilter.addDataScheme("package")
        context.registerReceiver(this, intentFilter)
    }

    companion object {
        private const val TAG = "AppInstallReceiver"
        private val logObjectTag = ObjectTag(ObjectTag.core, TAG)
    }
}