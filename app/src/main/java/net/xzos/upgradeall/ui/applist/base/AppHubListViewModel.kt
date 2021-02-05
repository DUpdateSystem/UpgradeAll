package net.xzos.upgradeall.ui.applist.base

import android.app.Application
import androidx.lifecycle.MutableLiveData
import net.xzos.upgradeall.core.data.ANDROID_APP_TYPE
import net.xzos.upgradeall.core.data.ANDROID_MAGISK_MODULE_TYPE
import net.xzos.upgradeall.core.manager.AppManager
import net.xzos.upgradeall.core.module.app.App
import net.xzos.upgradeall.ui.base.recycleview.ListContainerViewModel


abstract class AppHubListViewModel<L : BaseAppListItemView>(application: Application) : ListContainerViewModel<L>(application) {

    private val mAppType = MutableLiveData<String>().apply {
        this.observeForever {
            loadData()
        }
    }

    internal fun setAppType(appType: String) {
        mAppType.value = appType
    }

    fun getAppList(): List<App> {
        mAppType.value?.run {
            return if (this == ANDROID_APP_TYPE)
                AppManager.getAppListWithoutKey(ANDROID_MAGISK_MODULE_TYPE)
            else
                AppManager.getAppList(this)
        }?: return emptyList()
    }
}