package net.xzos.upgradeall.ui.applist.base

import android.app.Application
import net.xzos.upgradeall.core.data.ANDROID_APP_TYPE
import net.xzos.upgradeall.core.data.ANDROID_MAGISK_MODULE_TYPE
import net.xzos.upgradeall.core.manager.AppManager
import net.xzos.upgradeall.core.module.app.App
import net.xzos.upgradeall.ui.base.recycleview.ListContainerViewModel


abstract class AppHubListViewModel<L : BaseAppListItemView>(application: Application)
    : ListContainerViewModel<L>(application) {

    lateinit var mAppType: String

    protected fun getAppList(): List<App> {
        return if (mAppType == ANDROID_APP_TYPE)
            AppManager.getAppListWithoutKey(ANDROID_MAGISK_MODULE_TYPE)
        else
            AppManager.getAppList(mAppType)
    }
}