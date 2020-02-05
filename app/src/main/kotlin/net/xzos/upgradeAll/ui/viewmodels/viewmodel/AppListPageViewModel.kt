package net.xzos.upgradeAll.ui.viewmodels.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import net.xzos.upgradeAll.data.json.gson.UIConfig.Companion.uiConfig
import net.xzos.upgradeAll.server.app.manager.AppManager
import net.xzos.upgradeAll.server.app.manager.module.App
import net.xzos.upgradeAll.server.update.UpdateManager
import net.xzos.upgradeAll.ui.viewmodels.pageradapter.AppTabSectionsPagerAdapter.Companion.ALL_APP_PAGE_INDEX
import net.xzos.upgradeAll.ui.viewmodels.pageradapter.AppTabSectionsPagerAdapter.Companion.UPDATE_PAGE_INDEX
import net.xzos.upgradeAll.ui.viewmodels.pageradapter.AppTabSectionsPagerAdapter.Companion.USER_STAR_PAGE_INDEX
import net.xzos.upgradeAll.ui.viewmodels.view.ItemCardView
import net.xzos.upgradeAll.ui.viewmodels.view.ItemCardViewExtraData


class AppListPageViewModel : ViewModel() {

    private val mTabPageIndex = MutableLiveData<Int>()
    internal val appCardViewList: LiveData<MutableList<ItemCardView>> = Transformations.map(mTabPageIndex) { tabPageIndex ->
        return@map mutableListOf<ItemCardView>().apply {
            val apps: HashSet<App> = when (tabPageIndex) {
                UPDATE_PAGE_INDEX -> {
                    UpdateManager.blockRenewAll()
                }
                ALL_APP_PAGE_INDEX -> {
                    AppManager.getApps()
                }
                else -> {
                    val itemList =
                            if (tabPageIndex == USER_STAR_PAGE_INDEX)
                                uiConfig.userStarTab.itemList
                            else uiConfig.userTabList[tabPageIndex].itemList
                    hashSetOf<App>().apply {
                        for (item in itemList) {
                            if (item.type == "app")
                                AppManager.getApp(databaseId = item.appIdList[0])?.let {
                                    this.add(it)
                                }
                        }
                    }
                }
            }
            for (app in apps) {
                this.add(getAppItemCardView(app))
            }
            if (apps.isNotEmpty()) {
                this.add(ItemCardView())
            }
        }
    }

    internal fun setTabPageIndex(tabPageIndex: Int) {
        mTabPageIndex.value = tabPageIndex
    }

    private fun getAppItemCardView(app: App): ItemCardView {
        return ItemCardView(
                app.appDatabase.name,
                app.appDatabase.url,
                extraData = ItemCardViewExtraData(app = app))
    }

    internal val needUpdateAppsLiveLiveData = MutableLiveData(mutableListOf<App>())
}
