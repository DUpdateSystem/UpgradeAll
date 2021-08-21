package net.xzos.upgradeall.core.utils.android_app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import kotlinx.coroutines.runBlocking
import net.xzos.upgradeall.core.androidutils.androidContext
import net.xzos.upgradeall.core.coreConfig
import net.xzos.upgradeall.core.data.ANDROID_APP_TYPE
import net.xzos.upgradeall.core.manager.AppManager
import net.xzos.upgradeall.core.manager.HubManager


class AppReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!HubManager.isEnableApplicationsMode()) return
        val packageName = intent.data.toString()
        runBlocking {
            when (intent.action) {
                Intent.ACTION_PACKAGE_ADDED -> addApp(context, packageName)
                Intent.ACTION_PACKAGE_REPLACED -> renewApp(packageName)
                Intent.ACTION_PACKAGE_REMOVED -> delApp(packageName)
            }
        }
    }

    fun register() {
        val intentFilter = IntentFilter()
        intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED)
        intentFilter.addAction(Intent.ACTION_PACKAGE_REPLACED)
        intentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED)
        intentFilter.addDataScheme("package")
        androidContext.registerReceiver(this, intentFilter)
    }

    companion object {
        private suspend fun addApp(context: Context, packageName: String) {
            val appInfo = try {
                val pm = context.packageManager
                val applicationInfo = pm.getApplicationInfo(packageName, 0)
                val name = pm.getApplicationLabel(applicationInfo)
                AppInfo(name.toString(), mapOf(ANDROID_APP_TYPE to packageName))
            } catch (e: Throwable) {
                return
            }
            AppManager.updateApp(appInfo.toAppEntity())
        }

        private suspend fun renewApp(packageName: String) {
            AppManager.getAppList(ANDROID_APP_TYPE)
                .filter { it.appId.values.contains(packageName) }.forEach {
                    AppManager.renewApp(it)
                }
        }

        private suspend fun delApp(packageName: String) {
            val appId = getAppId(packageName)
            AppManager.getAppById(appId)?.run {
                AppManager.removeApp(this)
            }
        }

        private fun getAppId(packageName: String): Map<String, String> =
            mapOf(ANDROID_APP_TYPE to packageName)
    }
}