package net.xzos.upgradeall.ui.applist.base

import android.app.Application
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.xzos.upgradeall.core.manager.AppManager
import net.xzos.upgradeall.core.module.AppStatus
import net.xzos.upgradeall.core.module.app.App
import net.xzos.upgradeall.core.utils.constant.ANDROID_APP_TYPE
import net.xzos.upgradeall.core.utils.constant.ANDROID_MAGISK_MODULE_TYPE
import net.xzos.upgradeall.core.utils.coroutines.CoroutinesMutableList
import net.xzos.upgradeall.core.utils.coroutines.coroutinesMutableListOf
import net.xzos.upgradeall.ui.base.recycleview.ListContainerViewModel
import net.xzos.upgradeall.ui.base.recycleview.RecyclerViewAdapter.Companion.ADD
import net.xzos.upgradeall.ui.base.recycleview.RecyclerViewAdapter.Companion.CHANGE
import net.xzos.upgradeall.ui.base.recycleview.RecyclerViewAdapter.Companion.DEL
import net.xzos.upgradeall.ui.data.livedata.AppViewModel
import net.xzos.upgradeall.wrapper.core.upgrade


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
            if (mTabIndex == TabIndex.TAB_APPLICATIONS_APP) {
                if (app.releaseStatus == AppStatus.NETWORK_ERROR)
                    adapterDelete(app)
            } else {
                adapterChange(app)
            }
        }
    }

    private lateinit var mAppType: String
    private lateinit var mTabIndex: TabIndex

    fun initData(appType: String, tabIndex: TabIndex) {
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

    private fun getAppList(): Set<App> {
        return AppManager.appList
    }

    override suspend fun doLoadData(): List<App> {
        val list = getAppList().filter { checkAppInfo(it) }
        appList.resetList(sortList(list))
        return appList
    }

    private fun sortList(list: List<App>): List<App> {
        return if (mTabIndex == TabIndex.TAB_UPDATE || mTabIndex == TabIndex.TAB_APPLICATIONS_APP)
            list.sortedBy { it.name }
        else list
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
            TabIndex.TAB_UPDATE -> app.releaseStatus == AppStatus.APP_OUTDATED
            TabIndex.TAB_STAR -> app.star
            TabIndex.TAB_ALL -> !app.isVirtual
            TabIndex.TAB_APPLICATIONS_APP -> app.isVirtual
                    && (app.isRenewing || app.releaseStatus != AppStatus.NETWORK_ERROR)
        }
    }

    fun upgradeAll(context: Context) {
        GlobalScope.launch(Dispatchers.IO) {
            getLiveData().value?.first?.map { app ->
                launch {
                    app.upgrade(context)
                }
            }
        }
    }
}