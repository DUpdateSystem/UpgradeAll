package net.xzos.upgradeall.application

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.xzos.upgradeall.core.androidutils.initContext
import net.xzos.upgradeall.core.downloader.setDownloadServer
import net.xzos.upgradeall.core.manager.CloudConfigGetter
import net.xzos.upgradeall.data.PreferencesMap
import net.xzos.upgradeall.server.downloader.DownloadNotification
import net.xzos.upgradeall.utils.egg.egg
import net.xzos.upgradeall.utils.file.refreshStorage

fun initCore() {
    refreshStorage()
    initObject()
    PreferencesMap.sync()
    GlobalScope.launch { renewData() }
    egg()
}

private fun initObject() {
    initContext(MyApplication.context)
    setDownloadServer(DownloadNotification.downloadServiceNotificationMaker)
}

private suspend fun renewData() {
    if (PreferencesMap.auto_update_hub_config) {
        CloudConfigGetter.renewAllHubConfigFromCloud()
    }
    if (PreferencesMap.auto_update_app_config) {
        CloudConfigGetter.renewAllAppConfigFromCloud()
    }
}