package net.xzos.upgradeall.ui.applist.base

import android.app.Application
import net.xzos.upgradeall.core.data.ANDROID_APP_TYPE
import net.xzos.upgradeall.core.data.ANDROID_MAGISK_MODULE_TYPE
import net.xzos.upgradeall.core.data.json.uiConfig
import net.xzos.upgradeall.core.database.table.isInit
import net.xzos.upgradeall.core.manager.AppManager
import net.xzos.upgradeall.core.module.app.App
import net.xzos.upgradeall.core.module.app.Updater
import net.xzos.upgradeall.core.utils.coroutines.CoroutinesMutableList
import net.xzos.upgradeall.core.utils.coroutines.coroutinesMutableListOf
import net.xzos.upgradeall.ui.base.recycleview.ListContainerViewModel
import net.xzos.upgradeall.ui.base.recycleview.RecyclerViewAdapter.Companion.ADD
import net.xzos.upgradeall.ui.base.recycleview.RecyclerViewAdapter.Companion.CHANGE
import net.xzos.upgradeall.ui.base.recycleview.RecyclerViewAdapter.Companion.DEL
import net.xzos.upgradeall.ui.data.livedata.AppViewModel


class AppHubViewModel(application: Application) : ListContainerViewModel<App>(application) {

    private val appList: CoroutinesMutableList<App> = coroutinesMutableListOf()

    private val appViewModel = object : AppViewModel() {
        override fun appAdded(app: App) {
            adapterAdd(app)
        }

        override fun appChanged(app: App) {
            adapterChange(app)
        }

        override fun appDeleted(app: App) {
            adapterDelete(app)
        }

        override fun appUpdating(app: App) {
            adapterChange(app)
        }

        override fun appUpdated(app: App) {
            if (mTabIndex == TAB_APPLICATIONS_APP) {
                if (app.getReleaseStatus() == Updater.NETWORK_ERROR)
                    adapterDelete(app)
            } else {
                adapterChange(app)
            }
        }
    }

    private lateinit var mAppType: String
    private var mTabIndex: Int = 0

    fun initData(appType: String, tabIndex: Int) {
        mAppType = appType
        mTabIndex = tabIndex
    }

    private fun adapterAdd(app: App) {
        if (!checkAppInfo(app)) return
        val list = appList
        var position = list.indexOf(app)
        if (position == -1) {
            position = list.size
            list.add(app)
        }
        setList(list, position, ADD)
    }

    private fun adapterDelete(app: App) {
        val list = appList
        val position = list.indexOf(app)
        if (position != -1) {
            list.removeAt(position)
            setList(list, position, DEL)
        }
    }

    private fun adapterChange(app: App) {
        val list = appList
        val position = list.indexOf(app)
        if (position != -1)
            setList(list, position, CHANGE)
    }

    override fun onCleared() {
        appViewModel.clearObserve()
        super.onCleared()
    }

    private fun getAppList(): List<App> {
        return AppManager.getAppList()
    }

    override suspend fun doLoadData(): List<App> {
        val allList = getAppList()
        appList.clear()
        appList.addAll(allList.filter { checkAppInfo(it) })
        return appList
    }

    private fun checkAppInfo(app: App): Boolean {
        return checkAppType(app) && checkAppStatus(app)
    }

    private fun checkAppType(app: App): Boolean {
        val appId = app.appId
        return when (mAppType) {
            ANDROID_APP_TYPE -> !appId.containsKey(ANDROID_MAGISK_MODULE_TYPE)
            ANDROID_MAGISK_MODULE_TYPE -> appId.containsKey(ANDROID_MAGISK_MODULE_TYPE)
            else -> true
        }
    }

    private fun checkAppStatus(app: App): Boolean {
        return when (mTabIndex) {
            TAB_UPDATE -> app.getReleaseStatus() == Updater.APP_OUTDATED
            TAB_STAR -> uiConfig.userStarAppIdList.contains(app.appId)
            TAB_ALL -> app.appDatabase.isInit()
            TAB_APPLICATIONS_APP -> !app.appDatabase.isInit()
            else -> false
        }
    }
}