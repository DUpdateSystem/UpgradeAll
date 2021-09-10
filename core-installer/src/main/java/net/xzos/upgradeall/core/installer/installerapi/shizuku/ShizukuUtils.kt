package net.xzos.upgradeall.core.installer.installerapi.shizuku

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.IBinder
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale
import androidx.core.content.ContextCompat.checkSelfPermission
import net.xzos.upgradeall.core.installer.BuildConfig
import net.xzos.upgradeall.core.utils.log.Log
import net.xzos.upgradeall.core.utils.log.ObjectTag
import net.xzos.upgradeall.core.utils.log.ObjectTag.Companion.core
import rikka.shizuku.Shizuku
import rikka.shizuku.Shizuku.UserServiceArgs
import rikka.shizuku.ShizukuProvider
import rikka.sui.Sui


class ShizukuUtils(context: Context) {
    private val userServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, binder: IBinder?) {
            val res = java.lang.StringBuilder()
            res.append("onServiceConnected: ").append(componentName.className).append('\n')
            if (binder != null && binder.pingBinder()) {
                res.append("binder got")
            } else {
                res.append("invalid binder for ").append(componentName).append("received")
            }
            Log.i(logObjectTag, TAG, "onServiceConnected: $res")
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
        }
    }
    private val userServiceStandaloneProcessArgs = UserServiceArgs(
        ComponentName(
            context.packageName,
            this::class.java.name
        )
    ).processNameSuffix("shizuku_installer")
        .debuggable(BuildConfig.DEBUG)
        .version(BuildConfig.SHIZUKU_VERSION_CODE)

    fun bindUserServiceStandaloneProcess() {
        val res = StringBuilder()
        try {
            if (Shizuku.getVersion() < 10) {
                res.append("requires Shizuku API 10")
            } else {
                Shizuku.bindUserService(userServiceStandaloneProcessArgs, userServiceConnection)
            }
        } catch (tr: Throwable) {
            tr.printStackTrace()
            res.append(tr.toString())
        }
        Log.i(logObjectTag, TAG, "bindUserServiceStandaloneProcess: $res")
    }

    fun unbindUserServiceStandaloneProcess() {
        val res = java.lang.StringBuilder()
        try {
            if (Shizuku.getVersion() < 10) {
                res.append("requires Shizuku API 10")
            } else {
                Shizuku.unbindUserService(
                    userServiceStandaloneProcessArgs,
                    userServiceConnection,
                    true
                )
            }
        } catch (tr: Throwable) {
            tr.printStackTrace()
            res.append(tr.toString())
        }
        Log.i(logObjectTag, TAG, "unbindUserServiceStandaloneProcess: $res")
    }

    companion object {
        private const val TAG = "ShizukuUtils"
        private val logObjectTag = ObjectTag(core, TAG)

        fun initSui(context: Context) {
            Sui.init(context.packageName)
        }

        fun checkPermission(activity: Activity, code: Int): Boolean {
            try {
                initSui(activity)
                return if (!Shizuku.isPreV11() && Shizuku.getVersion() >= 11) {
                    // Sui and Shizuku >= 11 use self-implemented permission
                    if (Shizuku.checkSelfPermission() == PERMISSION_GRANTED) {
                        true
                    } else if (Shizuku.shouldShowRequestPermissionRationale()) {
                        Log.i(
                            logObjectTag, TAG,
                            "checkPermission: User denied permission (shouldShowRequestPermissionRationale=true)"
                        )
                        false
                    } else {
                        Shizuku.requestPermission(code)
                        false
                    }
                } else {
                    // Shizuku < 11 uses runtime permission
                    if (checkSelfPermission(
                            activity,
                            ShizukuProvider.PERMISSION
                        ) == PERMISSION_GRANTED
                    ) {
                        true
                    } else if (shouldShowRequestPermissionRationale(
                            activity,
                            ShizukuProvider.PERMISSION
                        )
                    ) {
                        Log.i(
                            logObjectTag, TAG,
                            "checkPermission: User denied permission (shouldShowRequestPermissionRationale=true)"
                        )
                        false
                    } else {
                        requestPermissions(activity, arrayOf(ShizukuProvider.PERMISSION), code)
                        false
                    }
                }
            } catch (e: Throwable) {
                Log.e(logObjectTag, TAG, "checkPermission: ${e.stackTraceToString()}")
            }
            return false
        }
    }
}