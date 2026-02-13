package net.xzos.upgradeall.application

import kotlinx.coroutines.launch
import net.xzos.upgradeall.app.backup.initBackupContext
import net.xzos.upgradeall.core.androidutils.initCoreContext
import net.xzos.upgradeall.core.manager.CloudConfigGetter
import net.xzos.upgradeall.data.PreferencesMap
import net.xzos.upgradeall.utils.egg.egg
import net.xzos.upgradeall.utils.file.refreshDataAndStatus

fun initCore() {
    refreshDataAndStatus()
    initObject()
    PreferencesMap.sync()
    MyApplication.applicationScope.launch { renewData() }
    egg()
}

private fun initObject() {
    initContext()
}

private fun initContext() {
    val context = MyApplication.context
    initCoreContext(context)
    initBackupContext(context)
}

private suspend fun renewData() {
    if (PreferencesMap.auto_update_hub_config) {
        CloudConfigGetter.renewAllHubConfigFromCloud()
    }
    if (PreferencesMap.auto_update_app_config) {
        CloudConfigGetter.renewAllAppConfigFromCloud()
    }
}