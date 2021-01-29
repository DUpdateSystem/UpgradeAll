package net.xzos.upgradeall.ui.viewmodels.viewmodel

import android.app.Application
import androidx.lifecycle.MutableLiveData
import net.xzos.upgradeall.core.manager.AppManager
import net.xzos.upgradeall.core.module.app.Updater
import net.xzos.upgradeall.ui.viewmodels.view.AppListItemView


class AppHubListViewModel(application: Application) : ListContainerViewModel<AppListItemView>(application) {

    private val mAppType = MutableLiveData<String>().apply {
        this.observeForever {
            loadData()
        }
    }
    private val mTabPageIndex = MutableLiveData(0).apply {
        this.observeForever {
            loadData()
        }
    }

    internal fun setTabPageIndex(tabPageIndex: Int) {
        mTabPageIndex.value = tabPageIndex
    }

    internal fun setAppType(appType: String) {
        mAppType.value = appType
    }

    override fun doLoadData(): List<AppListItemView> {
        mAppType.value?.run {
            return AppManager.getAppList(this)
                    .filter {
                        if (mTabPageIndex.value == 0)
                            it.getReleaseStatus() == Updater.APP_OUTDATED
                        else true
                    }.map { AppListItemView(it) }
        }
        return emptyList()
    }
}