package net.xzos.upgradeall.ui.applist.base

import android.app.Application
import net.xzos.upgradeall.core.data.ANDROID_APP_TYPE
import net.xzos.upgradeall.core.data.ANDROID_MAGISK_MODULE_TYPE
import net.xzos.upgradeall.core.data.json.uiConfig
import net.xzos.upgradeall.core.manager.AppManager
import net.xzos.upgradeall.core.manager.AppManager.DATA_UPDATE_NOTIFY
import net.xzos.upgradeall.core.module.app.App
import net.xzos.upgradeall.core.module.app.Updater
import net.xzos.upgradeall.core.utils.coroutines.CoroutinesMutableMap
import net.xzos.upgradeall.core.utils.coroutines.coroutinesMutableMapOf
import net.xzos.upgradeall.core.utils.oberver.ObserverFun
import net.xzos.upgradeall.ui.base.recycleview.ListContainerViewModel
import net.xzos.upgradeall.ui.base.recycleview.RecyclerViewAdapter.Companion.ADD
import net.xzos.upgradeall.ui.base.recycleview.RecyclerViewAdapter.Companion.DEL
import net.xzos.upgradeall.ui.data.livedata.AppViewModel


class AppHubViewModel(application: Application) : ListContainerViewModel<App>(application), AppViewModel {

    override val appList: CoroutinesMutableMap<App, Pair<ObserverFun<App>, ObserverFun<App>>> = coroutinesMutableMapOf()

    private lateinit var mAppType: String
    private var mTabIndex: Int = 0

    private val renewObserver: ObserverFun<App> = {
        loadData()
    }

    private val delObserver: ObserverFun<App> = {
        loadData()
    }

    private val addObserver: ObserverFun<App> = {
        loadData()
    }

    fun initSetting(appType: String, tabIndex: Int) {
        mAppType = appType
        mTabIndex = tabIndex
        when (mTabIndex) {
            TAB_UPDATE -> listOf(APP_CHANGED_NOTIFY, DATA_UPDATE_NOTIFY)
            else -> listOf(APP_CHANGED_NOTIFY)
        }.forEach {
            AppManager.observeForever(it, observer)
        }
    }

    private fun adapterAdd(app: App) {
        addObserver()
        val list = appList.keys.toList()
        setList(list, position, ADD)
    }

    private fun adapterDelete(position: Int) {
        val list = appList.keys.toList()
        setList(list, position, DEL)
    }

    private fun adapterChange(position: Int) {
        val list = appList.keys.toList()
        setList(list, position, DEL)
    }

    private fun checkAppChanged(app: App, changedTag: String) {
        val list = appList.keys.toList()
        val position = list.indexOf(app)
        setList(list, position)
    }

    private fun checkAppListChanged(appList: List<App>) {
        val list = this.appList.keys.toList()
        val position = list.indexOf(app)
        setList(list, position)
    }

    override fun onCleared() {
        AppManager.removeObserver(observer)
        clearObserve()
        super.onCleared()
    }

    private fun getAppList(): List<App> {
        return if (mAppType == ANDROID_APP_TYPE)
            AppManager.getAppListWithoutKey(ANDROID_MAGISK_MODULE_TYPE)
        else
            AppManager.getAppList(mAppType)
    }

    private fun addApp(app:App): Int {
        if (appList.containsKey(app)) return -1
        else{
            addObserve(app, { checkAppChanged(it) }, { checkAppChanged(it) })
        }
    }

    override suspend fun doLoadData(): List<App> {
        val allList = getAppList()
        val list = when (mTabIndex) {
            TAB_UPDATE -> allList.filter { it.getReleaseStatus() == Updater.APP_OUTDATED }
            TAB_STAR -> allList.filter { uiConfig.userStarAppIdList.contains(it.appId) }
            else -> allList
        }
        list.forEach { app ->
            addObserve(app, { checkAppChanged(it) }, { checkAppChanged(it) })
        }
        return list
    }
}