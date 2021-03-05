package net.xzos.upgradeall.ui.applist.base

import android.app.Application
import net.xzos.upgradeall.core.data.ANDROID_APP_TYPE
import net.xzos.upgradeall.core.data.ANDROID_MAGISK_MODULE_TYPE
import net.xzos.upgradeall.core.data.json.uiConfig
import net.xzos.upgradeall.core.manager.AppManager
import net.xzos.upgradeall.core.module.app.App
import net.xzos.upgradeall.core.module.app.Updater
import net.xzos.upgradeall.core.utils.oberver.ObserverFun
import net.xzos.upgradeall.ui.base.recycleview.ListContainerViewModel


class AppHubViewModel(application: Application)
    : ListContainerViewModel<App>(application) {

    lateinit var mAppType: String
    var mTabIndex: Int = 0

    private val observer: ObserverFun<Unit> = fun(_) {
        loadData()
    }

    override fun onCleared() {
        AppManager.removeObserver(observer)
        super.onCleared()
    }

    private fun getAppList(): List<App> {
        AppManager.observeForever(observer)
        return if (mAppType == ANDROID_APP_TYPE)
            AppManager.getAppListWithoutKey(ANDROID_MAGISK_MODULE_TYPE)
        else
            AppManager.getAppList(mAppType)
    }

    override suspend fun doLoadData(): List<App> {
        val list = getAppList()
        return when (mTabIndex) {
            TAB_UPDATE -> list.filter { it.getReleaseStatus() == Updater.APP_OUTDATED }
            TAB_STAR -> list.filter { uiConfig.userStarAppIdList.contains(it.appId) }
            else -> list
        }
    }
}