package net.xzos.upgradeall.ui.applist.base

import android.app.Application
import androidx.lifecycle.MutableLiveData
import net.xzos.upgradeall.core.data.ANDROID_APP_TYPE
import net.xzos.upgradeall.core.data.ANDROID_MAGISK_MODULE_TYPE
import net.xzos.upgradeall.core.data.json.uiConfig
import net.xzos.upgradeall.core.manager.AppManager
import net.xzos.upgradeall.core.module.app.App
import net.xzos.upgradeall.core.module.app.Updater
import net.xzos.upgradeall.ui.base.recycleview.ListContainerViewModel


class AppHubViewModel(application: Application)
    : ListContainerViewModel<App>(application) {

    private val mTabIndex = MutableLiveData<Int>().apply {
        this.observeForever {
            loadData()
        }
    }
    private val mAppType = MutableLiveData<String>().apply {
        this.observeForever {
            loadData()
        }
    }

    fun setAppType(appType: String) {
        mAppType.value = appType
    }

    fun setTabIndex(tabIndex: Int) {
        mTabIndex.value = tabIndex
    }

    fun setAutoRenewFun() {
        AppManager.appMapStatusChangedFun = {
            loadData()
        }
    }

    private fun getAppList(): List<App> {
        mAppType.value?.run {
            return if (this == ANDROID_APP_TYPE)
                AppManager.getAppListWithoutKey(ANDROID_MAGISK_MODULE_TYPE)
            else
                AppManager.getAppList(this)
        } ?: return emptyList()
    }

    override suspend fun doLoadData(): List<App> {
        val list = getAppList()
        return when (mTabIndex.value) {
            TAB_UPDATE -> list.filter { it.getReleaseStatus() == Updater.APP_OUTDATED }
            TAB_STAR -> list.filter { uiConfig.userStarAppIdList.contains(it.appId) }
            else -> list
        }
    }
}