package net.xzos.upgradeall.core

import android.app.Activity
import android.app.Notification
import net.xzos.upgradeall.core.data.CoreConfig
import net.xzos.upgradeall.core.data.WebDavConfig
import net.xzos.upgradeall.core.downloader.DownloadService
import net.xzos.upgradeall.core.installer.ApkShizukuInstaller


lateinit var coreConfig: CoreConfig
lateinit var webDavConfig: WebDavConfig

fun initCore(
        _coreConfig: CoreConfig,
        _webDavConfig: WebDavConfig,
        notificationMaker: (() -> Pair<Int, Notification>)? = null,
        activity: Activity? = null,
) {
    coreConfig = _coreConfig
    webDavConfig = _webDavConfig
    notificationMaker?.run { DownloadService.setNotificationMaker(this) }
    activity?.run { ApkShizukuInstaller.initByActivity(this, 0) }
}