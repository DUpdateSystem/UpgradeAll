package net.xzos.upgradeall.ui.viewmodels.viewmodel

import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.runBlocking
import net.xzos.upgradeall.core.server_manager.module.app.App
import net.xzos.upgradeall.core.server_manager.module.applications.Applications
import net.xzos.upgradeall.data.AppUiDataManager
import net.xzos.upgradeall.data.gson.toItemListBean
import net.xzos.upgradeall.ui.viewmodels.pageradapter.AppTabSectionsPagerAdapter.Companion.ALL_APP_PAGE_INDEX
import net.xzos.upgradeall.ui.viewmodels.pageradapter.AppTabSectionsPagerAdapter.Companion.UPDATE_PAGE_INDEX
import net.xzos.upgradeall.utils.mutableLiveDataOf


class AppListPageViewModel : AppListContainerViewModel() {

    internal val editableTab = MutableLiveData(false)  // 修改分组

    private val mTabPageIndex = MutableLiveData<Int>().apply {
        this.observeForever { tabPageIndex ->
            editableTab.value = false
            if (tabPageIndex == UPDATE_PAGE_INDEX) {
                appListLiveData = mutableLiveDataOf()
                AppUiDataManager.getAppListLivaData(tabPageIndex).observeForever { apps ->
                    appListLiveData.value = mutableListOf<App>().apply {
                        for (app in apps) {
                            when (app) {
                                is App -> add(app)
                                is Applications -> addAll(runBlocking { app.getNeedUpdateAppList(block = false) })
                            }
                        }
                    }
                }
            } else {
                appListLiveData = AppUiDataManager.getAppListLivaData(tabPageIndex)
            }
        }
    }

    fun getTabPageIndex(): Int = mTabPageIndex.value!!

    fun removeItemFromTabPage(position: Int): Boolean {
        return AppUiDataManager.removeItemFromTabPage(position, mTabPageIndex.value!!)
    }

    fun moveItemToOtherTabPage(position: Int, tabPageIndex: Int): Boolean {
        return if (mTabPageIndex.value == ALL_APP_PAGE_INDEX) {
            AppUiDataManager.addItem(appListLiveData.value!![position].appDatabase.toItemListBean(), tabPageIndex)
        } else {
            AppUiDataManager.moveItemToOtherGroup(position, mTabPageIndex.value!!, tabPageIndex).also {
                if (it) removeItemFromTabPage(position)
            }
        }
    }

    internal fun setTabPageIndex(tabPageIndex: Int) {
        mTabPageIndex.value = tabPageIndex
    }
}
