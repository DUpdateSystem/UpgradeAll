package net.xzos.upgradeall.ui.viewmodels.viewmodel

import androidx.lifecycle.MutableLiveData
import net.xzos.upgradeall.data.AppUiDataManager
import net.xzos.upgradeall.data.gson.UIConfig
import net.xzos.upgradeall.data.gson.UIConfig.Companion.uiConfig


class AppListPageViewModel : AppListContainerViewModel() {

    internal val editableTab = MutableLiveData(false)  // 修改分组

    private val mTabPageIndex = MutableLiveData<Int>().apply {
        this.observeForever { tabPageIndex ->
            editableTab.value = false
            appListLiveData = AppUiDataManager.getAppListLivaData(tabPageIndex)
        }
    }

    fun removeItemFromTabPage(position: Int): Boolean {
        return AppUiDataManager.removeItemFromTabPage(position, mTabPageIndex.value!!)
    }

    fun moveItemToOtherTabPage(position: Int, tabPageIndex: Int): Boolean {
        return AppUiDataManager.moveItemToOtherGroup(position, mTabPageIndex.value!!, tabPageIndex)
    }

    internal fun setTabPageIndex(tabPageIndex: Int) {
        mTabPageIndex.value = tabPageIndex
    }

    fun getTabIndexList(): List<UIConfig.CustomContainerTabListBean> = mutableListOf(uiConfig.userStarTab).apply {
        this.addAll(uiConfig.userTabList)
    }
}
