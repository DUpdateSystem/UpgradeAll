package net.xzos.upgradeall.core.installer

import android.app.Activity
import net.xzos.upgradeall.core.installer.status.appInstallReceiver

fun initConfig(activity: Activity) {
    appInstallReceiver.register(activity)
}
