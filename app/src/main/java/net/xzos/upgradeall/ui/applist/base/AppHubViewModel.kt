package net.xzos.upgradeall.ui.applist.base

import android.app.Application
import net.xzos.upgradeall.core.data.ANDROID_APP_TYPE
import net.xzos.upgradeall.core.data.ANDROID_MAGISK_MODULE_TYPE
import net.xzos.upgradeall.core.data.json.uiConfig
import net.xzos.upgradeall.core.manager.AppManager
import net.xzos.upgradeall.core.manager.AppManager.APP_CHANGED_NOTIFY
import net.xzos.upgradeall.core.manager.AppManager.DATA_UPDATE_NOTIFY
import net.xzos.upgradeall.core.module.app.App
import net.xzos.upgradeall.core.module.app.Updater
import net.xzos.upgradeall.core.utils.oberver.ObserverFun
import net.xzos.upgradeall.ui.base.recycleview.ListContainerViewModel


class AppHubViewModel(application: Application)
    : ListContainerViewModel<App>(application) {

    private lateinit var mAppType: String
    private var mTabIndex: Int = 0

    private val observer: ObserverFun<Unit> = fun(_) {
        loadData()
    }

    fun initSetting(appType: String, tabIndex: Int) {
        mAppType = appType
        mTabIndex = tabIndex
        when (mTabIndex) {
            TAB_UPDATE -> listOf(APP_CHANGED_NOTIFY, DATA_UPDATE_NOTIFY)
            else -> listOf(APP_CHANGED_NOTIFY)
        }.forEach {
            AppManager.observeForever(it, observer)
        }
    }

    override fun onCleared() {
        AppManager.removeObserver(observer)
        super.onCleared()
    }

    private fun getAppList(): List<App> {
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