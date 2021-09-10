package net.xzos.upgradeall.core.utils

import android.content.Context
import net.xzos.upgradeall.core.androidutils.app_info.AppInfo
import net.xzos.upgradeall.core.androidutils.app_info.getAndroidAppInfoList
import net.xzos.upgradeall.core.androidutils.app_info.getAndroidModuleInfoList
import net.xzos.upgradeall.core.database.table.AppEntity
import net.xzos.upgradeall.core.manager.HubManager

fun getInstalledAppList(context: Context, ignoreSystemApp:Boolean): List<AppEntity> {
    return if (HubManager.isEnableApplicationsMode()) {
        val appInfoLIst = getAndroidAppInfoList(context, ignoreSystemApp) + getAndroidModuleInfoList()
        return appInfoLIst.map { it.toAppEntity() }
    } else emptyList()
}

fun AppInfo.toAppEntity(): AppEntity {
    return AppEntity(name, idMap)
}