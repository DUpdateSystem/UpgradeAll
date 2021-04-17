package net.xzos.upgradeall.core.installer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import net.xzos.upgradeall.core.coreConfig
import net.xzos.upgradeall.core.log.Log
import net.xzos.upgradeall.core.log.ObjectTag
import net.xzos.upgradeall.core.log.msg


class AppInstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val manager = context.packageManager
        val packageName = intent.data!!.schemeSpecificPart

        try {
            val info = manager.getPackageInfo(packageName, 0)
            ApkInstaller.notifyCompleteInstall(info)
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(logObjectTag, TAG, "error: ${e.msg()}")
        }
    }


    fun register() {
        val intentFilter = IntentFilter()
        intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED)
        intentFilter.addAction(Intent.ACTION_PACKAGE_REPLACED)
        intentFilter.addDataScheme("package")
        coreConfig.androidContext.registerReceiver(this, intentFilter)
    }

    companion object {
        private const val TAG = "AppInstallReceiver"
        private val logObjectTag = ObjectTag(ObjectTag.core, TAG)
    }
}