package net.xzos.upgradeall.application

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.xzos.upgradeall.core.androidutils.initContext
import net.xzos.upgradeall.core.manager.CloudConfigGetter
import net.xzos.upgradeall.data.PreferencesMap
import net.xzos.upgradeall.utils.egg.egg

fun initCore() {
    initObject()
    PreferencesMap.sync()
    GlobalScope.launch { renewData() }
    egg()
}

private fun initObject() {
    initContext(MyApplication.context)
}

private suspend fun renewData() {
    if (PreferencesMap.auto_update_hub_config) {
        CloudConfigGetter.renewAllHubConfigFromCloud()
    }
    if (PreferencesMap.auto_update_app_config) {
        CloudConfigGetter.renewAllAppConfigFromCloud()
    }
}