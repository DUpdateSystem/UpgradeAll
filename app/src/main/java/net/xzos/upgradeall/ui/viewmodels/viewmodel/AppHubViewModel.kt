package net.xzos.upgradeall.ui.viewmodels.viewmodel

import androidx.lifecycle.MutableLiveData
import net.xzos.upgradeall.core.manager.AppManager


class AppHubViewModel : AppListContainerViewModel() {

    val itemCountLiveData: MutableLiveData<Int> = MutableLiveData(0)

    private val mTabPageIndex = MutableLiveData<String>().apply {
        this.observeForever { appType ->
            AppManager.getAppList(appType)
        }
    }

    internal fun setAppType(appType: String) {
        mTabPageIndex.value = appType
    }
}