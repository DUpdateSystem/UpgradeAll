package net.xzos.upgradeall.core.utils.android_app

import net.xzos.upgradeall.core.coreConfig
import net.xzos.upgradeall.core.database.table.AppEntity

fun getInstalledAppList(): List<AppEntity> {
    return if (coreConfig.enableApplicationsMode) {
        val appInfoLIst = getAndroidAppInfoList() + getAndroidModuleInfoList()
        return appInfoLIst.map { it.toAppEntity() }
    } else emptyList()
}