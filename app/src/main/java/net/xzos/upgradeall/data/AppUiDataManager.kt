package net.xzos.upgradeall.data

import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.runBlocking
import net.xzos.upgradeall.core.oberver.ObserverFun
import net.xzos.upgradeall.core.server_manager.AppManager
import net.xzos.upgradeall.core.server_manager.UpdateManager
import net.xzos.upgradeall.core.server_manager.module.BaseApp
import net.xzos.upgradeall.core.server_manager.module.app.App
import net.xzos.upgradeall.core.server_manager.module.applications.Applications
import net.xzos.upgradeall.data.gson.UIConfig
import net.xzos.upgradeall.data.gson.UIConfig.Companion.uiConfig
import net.xzos.upgradeall.ui.viewmodels.pageradapter.AppTabSectionsPagerAdapter.Companion.ALL_APP_PAGE_INDEX
import net.xzos.upgradeall.ui.viewmodels.pageradapter.AppTabSectionsPagerAdapter.Companion.UPDATE_PAGE_INDEX
import net.xzos.upgradeall.ui.viewmodels.pageradapter.AppTabSectionsPagerAdapter.Companion.USER_STAR_PAGE_INDEX
import net.xzos.upgradeall.utils.mutableLiveDataOf
import net.xzos.upgradeall.utils.notifyObserver
import net.xzos.upgradeall.utils.setValueBackground

object AppUiDataManager {
    // 需要升级的 APP
    private val needUpdateAppListLiveData: MutableLiveData<List<BaseApp>> = mutableLiveDataOf()

    // 所有应用列表
    private val allAppListLiveData: MutableLiveData<List<BaseApp>> = mutableLiveDataOf<List<BaseApp>>().apply {
        value = AppManager.apps
    }

    // 列表中所有的 APP
    private val appListLiveDataMap: MutableMap<Int, MutableLiveData<List<BaseApp>>> = mutableMapOf()

    private val applicationObserverFunMap: MutableMap<Applications, ObserverFun<Unit>> = mutableMapOf()

    init {
        AppManager.observeForever<Unit>(fun(_) {
            refreshAllAppListMap()
        })
        val updateObserver: ObserverFun<Unit> = fun(_: Unit) {
            refreshNeedUpdateAppList()
        }
        UpdateManager.observeForever(UpdateManager.UPDATE_STATUS_CHANGED, updateObserver)
        // 初始化绑定 Map
        appListLiveDataMap[ALL_APP_PAGE_INDEX] = allAppListLiveData
        appListLiveDataMap[UPDATE_PAGE_INDEX] = needUpdateAppListLiveData
        refreshAppListMap()
    }

    private fun refreshNeedUpdateAppList() {
        val needUpdateAppList = runBlocking { UpdateManager.getNeedUpdateAppList(block = false) }
        val list: List<App> = mutableListOf<App>().apply {
            for (baseApp in needUpdateAppList) {
                when (baseApp) {
                    is App -> add(baseApp)
                    is Applications -> addAll(runBlocking { baseApp.needUpdateAppList })
                }
            }
        }
        setApplicationObserverMap(needUpdateAppList.filterIsInstance<Applications>())
        needUpdateAppListLiveData.setValueBackground(list)
    }

    private fun setApplicationObserverMap(newList: List<Applications>) {
        val oldList = applicationObserverFunMap.keys.toList()
        if (oldList == newList) return
        val delList = oldList.filter {
            !newList.contains(it)
        }
        for (applications in delList) {
            applications.removeObserver(applicationObserverFunMap.remove(applications)!!)
        }
        val addList = newList.filter {
            !oldList.contains(it)
        }
        for (applications in addList) {
            val observerFun = fun(_: Unit) {
                refreshNeedUpdateAppList()
            }.also {
                applicationObserverFunMap[applications] = it
            }
            applications.observeForever(observerFun)
        }
    }

    private fun refreshAllAppListMap() {
        allAppListLiveData.setValueBackground(AppManager.apps)
    }

    private fun refreshAppListMap() {
        appListLiveDataMap[USER_STAR_PAGE_INDEX] = MutableLiveData(
                uiConfig.userStarTab.itemList.mapNotNull {
                    AppManager.getBaseApp(it.appIdList[0])
                })
        val userTabList = uiConfig.userTabList
        for (tabPageIndex in 0 until userTabList.size) {
            appListLiveDataMap[tabPageIndex] = MutableLiveData(userTabList[tabPageIndex].itemList.mapNotNull {
                AppManager.getBaseApp(it.appIdList[0])
            })
        }
    }

    fun getAppListLivaData(tabPageIndex: Int): MutableLiveData<List<BaseApp>> {
        return appListLiveDataMap[tabPageIndex]!!
    }

    fun addUserTab(name: String, icon: String?): Boolean {
        return uiConfig.addUserTab(name, icon).also {
            if (it) refreshAppListMap()
        }
    }

    fun removeUserTab(position: Int? = null, userTabListBean: UIConfig.CustomContainerTabListBean? = null) {
        uiConfig.removeUserTab(position, userTabListBean)
        refreshAppListMap()
    }

    fun removeItemFromTabPage(position: Int, tabPageIndex: Int): Boolean {
        return uiConfig.removeItemFromTabPage(position, tabPageIndex).also {
            val appLiveData = appListLiveDataMap[tabPageIndex] ?: return false
            appLiveData.remove(appLiveData.value!![position])
        }
    }

    fun swapUserTabOrder(fromPosition: Int, toPosition: Int): Boolean {
        return uiConfig.swapUserTabOrder(fromPosition, toPosition).also {
            if (it) {
                appListLiveDataMap[fromPosition]!!.value = appListLiveDataMap[toPosition]!!.value
                        .also { appListLiveDataMap[toPosition]!!.value = appListLiveDataMap[fromPosition]!!.value }
            }
        }
    }

    fun addItem(itemListBean: UIConfig.CustomContainerTabListBean.ItemListBean, tabPageIndex: Int): Boolean {
        return uiConfig.addItem(itemListBean, tabPageIndex).also {
            AppManager.getBaseApp(itemListBean.appIdList[0])?.let {
                appListLiveDataMap[tabPageIndex]?.add(it)
            }
        }
    }

    fun moveItemToOtherGroup(position: Int, fromTabPageIndex: Int, toTabPageIndex: Int): Boolean {
        return uiConfig.moveItemToOtherGroup(position, fromTabPageIndex, toTabPageIndex).also {
            if (it) {
                val item = appListLiveDataMap[fromTabPageIndex]!!.value!![position]
                appListLiveDataMap[fromTabPageIndex]?.remove(item)
                appListLiveDataMap[toTabPageIndex]?.add(item)
            }
        }
    }

    private fun <T> MutableLiveData<List<T>>.add(item: T): Boolean {
        var bool = false
        this.value = value?.toMutableList()?.apply {
            bool = add(item)
        }?.toList()
        return bool
    }

    private fun <T> MutableLiveData<List<T>>.remove(item: T): Boolean {
        var bool = false
        this.value = value?.toMutableList()?.apply {
            bool = remove(item)
        }?.toList()
        return bool
    }

    fun apply() {
        for (mutableLiveData in appListLiveDataMap.values)
            mutableLiveData.notifyObserver()
    }
}
