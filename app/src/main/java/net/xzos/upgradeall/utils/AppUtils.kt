package net.xzos.upgradeall.utils

import android.content.pm.PackageManager
import android.text.TextUtils
import net.xzos.upgradeall.application.MyApplication

object AppUtils {
    fun isAppInstalled(pkgName: String): Boolean {
        return if (TextUtils.isEmpty(pkgName)) {
            false
        } else {
            val pm: PackageManager = MyApplication.context.packageManager
            try {
                pm.getApplicationInfo(pkgName, 0).enabled
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }
        }
    }
}