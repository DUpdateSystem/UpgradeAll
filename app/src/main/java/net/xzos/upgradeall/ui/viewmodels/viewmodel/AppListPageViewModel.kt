package net.xzos.upgradeall.ui.viewmodels.viewmodel

import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.*
import net.xzos.upgradeall.core.server_manager.AppManager
import net.xzos.upgradeall.core.server_manager.UpdateManager.Companion.updateManager
import net.xzos.upgradeall.core.server_manager.module.BaseApp
import net.xzos.upgradeall.core.server_manager.module.app.App
import net.xzos.upgradeall.core.server_manager.module.applications.Applications
import net.xzos.upgradeall.data_manager.UIConfig
import net.xzos.upgradeall.data_manager.UIConfig.Companion.APPLICATIONS_TYPE_TAG
import net.xzos.upgradeall.data_manager.UIConfig.Companion.APP_TYPE_TAG
import net.xzos.upgradeall.data_manager.UIConfig.Companion.uiConfig
import net.xzos.upgradeall.ui.viewmodels.pageradapter.AppTabSectionsPagerAdapter.Companion.ALL_APP_PAGE_INDEX
import net.xzos.upgradeall.ui.viewmodels.pageradapter.AppTabSectionsPagerAdapter.Companion.UPDATE_PAGE_INDEX
import net.xzos.upgradeall.ui.viewmodels.pageradapter.AppTabSectionsPagerAdapter.Companion.USER_STAR_PAGE_INDEX


class AppListPageViewModel : AppListContainerViewModel() {

    internal val editableTab = MutableLiveData(false)  // 修改分组

    private val mTabPageIndex = MutableLiveData<Int>().apply {
        this.observeForever { tabPageIndex ->
            editableTab.value = false
            GlobalScope.launch {
                val apps = getApps(tabPageIndex)
                setApps(apps)
            }
        }
    }

    private suspend fun getApps(tabPageIndex: Int): List<BaseApp> {
        return when (tabPageIndex) {
            UPDATE_PAGE_INDEX -> {
                val baseAppList = updateManager.getNeedUpdateAppList(block = true)
                mutableListOf<App>().apply {
                    for (baseApp in baseAppList) {
                        when (baseApp) {
                            is App -> add(baseApp)
                            is Applications -> addAll(baseApp.getNeedUpdateAppList(block = true))
                        }
                    }
                }
            }
            ALL_APP_PAGE_INDEX -> {
                AppManager.apps
            }
            else -> {
                withContext(Dispatchers.Main) {
                    editableTab.value = true
                }
                val itemList =
                        if (tabPageIndex == USER_STAR_PAGE_INDEX)
                            uiConfig.userStarTab.itemList
                        else uiConfig.userTabList[tabPageIndex].itemList
                mutableListOf<BaseApp>().apply {
                    for (item in itemList) {
                        (when (item.type) {
                            APP_TYPE_TAG -> {
                                AppManager.getSingleApp(databaseId = item.appIdList[0])
                            }
                            APPLICATIONS_TYPE_TAG -> {
                                AppManager.getApplications(databaseId = item.appIdList[0])
                            }
                            else -> null
                        })?.let {
                            this.add(it)
                        }

                    }
                }
            }
        }
    }

    fun removeItemFromGroup(position: Int): Boolean {
        val tabPageIndex = mTabPageIndex.value ?: return false
        if (tabPageIndex != UPDATE_PAGE_INDEX && tabPageIndex != ALL_APP_PAGE_INDEX) {
            if (tabPageIndex == USER_STAR_PAGE_INDEX) {
                uiConfig.userStarTab.itemList.removeAt(position)
            } else {
                uiConfig.userTabList[tabPageIndex].itemList.removeAt(position)
            }
            uiConfig.save()
            mTabPageIndex.value?.run {
                setTabPageIndex(this)
            }
            return true
        }
        return false
    }

    fun moveItemToOtherGroup(position: Int, containerTabListBean: UIConfig.CustomContainerTabListBean): Boolean {
        mTabPageIndex.value?.run {
            val app = if (this == ALL_APP_PAGE_INDEX || this == UPDATE_PAGE_INDEX)
                runBlocking { getApps(this@run) }.toList()[position]
            else null
            return uiConfig.moveItemToOtherGroup(position, this, containerTabListBean = containerTabListBean, app = app).also {
                if (it)
                    mTabPageIndex.value?.run {
                        setTabPageIndex(this)
                    }
            }
        }
        return false
    }

    internal fun setTabPageIndex(tabPageIndex: Int) {
        mTabPageIndex.value = tabPageIndex
    }

    fun getTabIndexList(): List<UIConfig.CustomContainerTabListBean> = mutableListOf(uiConfig.userStarTab).apply {
        this.addAll(uiConfig.userTabList)
    }

}
