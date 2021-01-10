package net.xzos.upgradeall.core

import android.app.Activity
import android.app.Notification
import net.xzos.upgradeall.core.data.CoreConfig
import net.xzos.upgradeall.core.downloader.DownloadService
import net.xzos.upgradeall.core.installer.ApkShizukuInstaller


lateinit var coreConfig: CoreConfig

fun initCore(
        coreConfig_: CoreConfig,
        notificationMaker: (() -> Pair<Int, Notification>)? = null,
        activity: Activity? = null,
) {
    coreConfig = coreConfig_
    notificationMaker?.run { DownloadService.setNotificationMaker(this) }
    activity?.run { ApkShizukuInstaller.initByActivity(this, 0) }
}