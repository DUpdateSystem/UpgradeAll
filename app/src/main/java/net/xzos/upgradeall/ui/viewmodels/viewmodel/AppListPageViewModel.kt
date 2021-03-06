package net.xzos.upgradeall.ui.viewmodels.viewmodel

import androidx.lifecycle.MutableLiveData
import net.xzos.upgradeall.data.AppUiDataManager
import net.xzos.upgradeall.data.gson.toItemListBean
import net.xzos.upgradeall.ui.viewmodels.pageradapter.AppTabSectionsPagerAdapter.Companion.ALL_APP_PAGE_INDEX


class AppListPageViewModel : AppListContainerViewModel() {

    internal val editableTab = MutableLiveData(false)  // 修改分组

    private val mTabPageIndex = MutableLiveData<Int>().apply {
        this.observeForever { tabPageIndex ->
            editableTab.value = false
            AppUiDataManager.getAppListLivaData(tabPageIndex).observeForever {
                setAppList(it)
            }
        }
    }

    internal fun setTabPageIndex(tabPageIndex: Int) {
        mTabPageIndex.value = tabPageIndex
    }

    fun getTabPageIndex(): Int = mTabPageIndex.value ?: 0

    fun removeItemFromTabPage(position: Int): Boolean {
        return AppUiDataManager.removeItemFromTabPage(position, mTabPageIndex.value!!)
    }

    fun moveItemToOtherTabPage(position: Int, tabPageIndex: Int): Boolean {
        return if (mTabPageIndex.value == ALL_APP_PAGE_INDEX) {
            AppUiDataManager.addItem(getAppList()[position].appDatabase.toItemListBean(), tabPageIndex)
        } else {
            AppUiDataManager.moveItemToOtherGroup(position, mTabPageIndex.value!!, tabPageIndex).also {
                if (it) removeItemFromTabPage(position)
            }
        }
    }
}
