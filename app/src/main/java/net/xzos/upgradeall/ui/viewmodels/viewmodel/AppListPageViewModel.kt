package net.xzos.upgradeall.ui.viewmodels.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.*
import net.xzos.dupdatesystem.server_manager.runtime.manager.AppManager
import net.xzos.dupdatesystem.server_manager.runtime.manager.module.app.App
import net.xzos.upgradeall.data_manager.UIConfig
import net.xzos.upgradeall.data_manager.UIConfig.Companion.APP_TYPE_TAG
import net.xzos.upgradeall.data_manager.UIConfig.Companion.uiConfig
import net.xzos.upgradeall.server.update.UpdateManager
import net.xzos.upgradeall.ui.viewmodels.pageradapter.AppTabSectionsPagerAdapter.Companion.ALL_APP_PAGE_INDEX
import net.xzos.upgradeall.ui.viewmodels.pageradapter.AppTabSectionsPagerAdapter.Companion.UPDATE_PAGE_INDEX
import net.xzos.upgradeall.ui.viewmodels.pageradapter.AppTabSectionsPagerAdapter.Companion.USER_STAR_PAGE_INDEX
import net.xzos.upgradeall.ui.viewmodels.view.ItemCardView
import net.xzos.upgradeall.ui.viewmodels.view.ItemCardViewExtraData


class AppListPageViewModel : ViewModel() {

    internal val needUpdateAppsLiveLiveData = MutableLiveData(mutableListOf<App>())
    internal val appCardViewList = MutableLiveData(mutableListOf<ItemCardView>())
    private val mTabPageIndex = MutableLiveData<Int>().apply {
        this.observeForever { tabPageIndex ->
            GlobalScope.launch {
                withContext(Dispatchers.Main) {
                    editableTab.value = false
                }
                val apps: HashSet<App> = getApps(tabPageIndex)
                val itemCardViewList = mutableListOf<ItemCardView>().apply {
                    for (app in apps) {
                        this.add(getAppItemCardView(app))
                    }
                    if (apps.isNotEmpty()) {
                        this.add(ItemCardView())
                    }
                }
                withContext(Dispatchers.Main) {
                    appCardViewList.value = itemCardViewList
                }
            }
        }
    }

    private suspend fun getApps(tabPageIndex: Int) = when (tabPageIndex) {
        UPDATE_PAGE_INDEX -> {
            UpdateManager.blockRenewAll()
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
            hashSetOf<App>().apply {
                for (item in itemList) {
                    if (item.type == APP_TYPE_TAG) {
                        AppManager.getSingleApp(databaseId = item.appIdList[0])?.let {
                            this.add(it)
                        }
                    }
                }
            }
        }
    }

    internal val editableTab = MutableLiveData(false)

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

    private fun getAppItemCardView(app: App): ItemCardView {
        return ItemCardView(
                app.appInfo.name,
                app.appInfo.url,
                extraData = ItemCardViewExtraData(app = app))
    }
}
