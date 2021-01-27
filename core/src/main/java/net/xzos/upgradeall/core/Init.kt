package net.xzos.upgradeall.core

import android.app.Activity
import android.app.Notification
import net.xzos.upgradeall.core.data.CoreConfig
import net.xzos.upgradeall.core.data.WebDavConfig
import net.xzos.upgradeall.core.downloader.DownloadService
import net.xzos.upgradeall.core.installer.ApkShizukuInstaller
import net.xzos.upgradeall.core.manager.AppManager
import net.xzos.upgradeall.core.manager.HubManager


lateinit var coreConfig: CoreConfig
lateinit var webDavConfig: WebDavConfig

/**
 * 初始化 Core 的配置，也用作用户修改设置后传入新的设置
 * @param _coreConfig Core 运行的核心设置
 * @param _webDavConfig WebDAV 备份功能相关设置
 * @param downloaderNotificationMaker 用于生成下载服务前台通知的函数
 * @param activity 启用 Shizuku 授权的 Activity，可如果为空，请在之后手动调用 {@link ApkShizukuInstaller#initByActivity}
 */
fun initCore(
        _coreConfig: CoreConfig,
        _webDavConfig: WebDavConfig,
        downloaderNotificationMaker: (() -> Pair<Int, Notification>)? = null,
        activity: Activity? = null,
) {
    coreConfig = _coreConfig
    webDavConfig = _webDavConfig
    downloaderNotificationMaker?.run { DownloadService.setNotificationMaker(this) }
    activity?.run { ApkShizukuInstaller.initByActivity(this, 0) }
    initObject()
}

// 提前初始化 Object，避免多线程时属性初始化错误
private fun initObject() {
    HubManager
    AppManager
}