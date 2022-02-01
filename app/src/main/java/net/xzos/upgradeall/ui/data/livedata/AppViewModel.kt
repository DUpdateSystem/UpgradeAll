package net.xzos.upgradeall.ui.data.livedata

import net.xzos.upgradeall.core.androidutils.runUiFun
import net.xzos.upgradeall.core.manager.AppManager
import net.xzos.upgradeall.core.manager.UpdateStatus
import net.xzos.upgradeall.core.module.app.App
import net.xzos.upgradeall.core.utils.coroutines.coroutinesMutableMapOf
import net.xzos.upgradeall.core.utils.oberver.ObserverFun

open class AppViewModel {
    private val appMap: MutableMap<App, List<ObserverFun<App>>> = coroutinesMutableMapOf(true)

    private val appAddedObserver: ObserverFun<App> = {
        runUiFun {
            appAdded(it)
            appMap[it]?.run {
                this[0](it)
            }
        }
    }
    private val appDeletedObserver: ObserverFun<App> = {
        runUiFun {
            appDeleted(it)
            appMap[it]?.run {
                this[1](it)
            }
        }
    }
    private val appChangedObserver: ObserverFun<App> = {
        runUiFun {
            appChanged(it)
            appMap[it]?.run {
                this[2](it)
            }
        }
    }
    private val appUpdatingObserver: ObserverFun<App> = {
        runUiFun {
            appUpdating(it)
            appMap[it]?.run {
                this[3](it)
            }
        }
    }

    private val appUpdateChangedObserver: ObserverFun<App> = {
        runUiFun {
            appUpdateChanged(it)
            appMap[it]?.run {
                this[3](it)
            }
        }
    }

    private val appUpdatedObserver: ObserverFun<App> = {
        runUiFun {
            appUpdated(it)
            appMap[it]?.run {
                this[3](it)
            }
        }
    }

    init {
        initObserve()
    }

    open fun appAdded(app: App) {}
    open fun appDeleted(app: App) {}
    open fun appChanged(app: App) {}

    open fun appUpdating(app: App) {}
    open fun appUpdateChanged(app: App) {}
    open fun appUpdated(app: App) {}

    private fun initObserve() {
        AppManager.observeForever(UpdateStatus.APP_START_UPDATE_NOTIFY, appUpdatingObserver)
        AppManager.observeForever(UpdateStatus.APP_UPDATE_STATUS_CHANGED_NOTIFY, appUpdateChangedObserver)
        AppManager.observeForever(UpdateStatus.APP_FINISH_UPDATE_NOTIFY, appUpdatedObserver)
        AppManager.observeForever(UpdateStatus.APP_ADDED_NOTIFY, appAddedObserver)
        AppManager.observeForever(UpdateStatus.APP_DATABASE_CHANGED_NOTIFY, appChangedObserver)
        AppManager.observeForever(UpdateStatus.APP_DELETED_NOTIFY, appDeletedObserver)
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