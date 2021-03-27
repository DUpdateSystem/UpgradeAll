package net.xzos.upgradeall.ui.data.livedata

import net.xzos.upgradeall.core.manager.AppManager
import net.xzos.upgradeall.core.module.app.App
import net.xzos.upgradeall.core.utils.coroutines.coroutinesMutableMapOf
import net.xzos.upgradeall.core.utils.oberver.ObserverFun

class AppViewModel {
    private val appMap: MutableMap<App, List<ObserverFun<App>>> = coroutinesMutableMapOf(true)

    private val appAddedObserver: ObserverFun<App> = {
        appAdded(it)
        appMap[it]?.run {
            this[0](it)
        }
    }
    private val appDeletedObserver: ObserverFun<App> = {
        appDeleted(it)
        appMap[it]?.run {
            this[1](it)
        }
    }
    private val appChangedObserver: ObserverFun<App> = {
        appChanged(it)
        appMap[it]?.run {
            this[2](it)
        }
    }
    private val appUpdatedObserver: ObserverFun<App> = {
        appUpdated(it)
        appMap[it]?.run {
            this[3](it)
        }
    }

    fun appAdded(app: App) {}

    fun appDeleted(app: App) {}

    fun appChanged(app: App) {}

    fun appUpdated(app: App) {}

    fun initObserve() {
        AppManager.observeForever(AppManager.DATA_UPDATE_NOTIFY, appUpdatedObserver)
        AppManager.observeForever(AppManager.APP_ADDED_NOTIFY, appAddedObserver)
        AppManager.observeForever(AppManager.APP_DATABASE_CHANGED_NOTIFY, appChangedObserver)
        AppManager.observeForever(AppManager.APP_DELETED_NOTIFY, appDeletedObserver)
    }

    fun addObserve(
            app: App,
            appAddedObserver: ObserverFun<App>, appDeletedObserver: ObserverFun<App>,
            appChangedObserver: ObserverFun<App>, appUpdatedObserver: ObserverFun<App>,
    ) {
        removeObserve(app)
        appMap[app] = listOf(appAddedObserver, appDeletedObserver, appChangedObserver, appUpdatedObserver)
    }

    fun removeObserve(app: App) {
        appMap.remove(app)
    }

    fun clearObserve() {
        AppManager.removeObserver(appAddedObserver)
        AppManager.removeObserver(appDeletedObserver)
        AppManager.removeObserver(appChangedObserver)
        AppManager.removeObserver(appUpdatedObserver)
    }

    fun updateData() {
        appMap.forEach { entry ->
            entry.value.forEach {
                it(entry.key)
            }
        }
    }
}