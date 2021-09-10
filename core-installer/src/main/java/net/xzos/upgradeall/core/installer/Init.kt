package net.xzos.upgradeall.core.installer

import android.content.Context
import net.xzos.upgradeall.core.installer.status.appInstallReceiver

fun initConfig(context: Context, installMode: String) {
    ApkInstaller.installMode = installMode
    appInstallReceiver.register(context)
}