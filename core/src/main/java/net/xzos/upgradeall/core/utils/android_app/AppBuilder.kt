package net.xzos.upgradeall.core.utils.android_app

import net.xzos.upgradeall.core.database.table.AppEntity
import net.xzos.upgradeall.core.manager.HubManager

fun getInstalledAppList(): List<AppEntity> {
    return if (HubManager.isApplicationsMode()) {
        val appInfoLIst = getAndroidAppInfoList() + getAndroidModuleInfoList()
        return appInfoLIst.map { it.toAppEntity() }
    } else emptyList()
}