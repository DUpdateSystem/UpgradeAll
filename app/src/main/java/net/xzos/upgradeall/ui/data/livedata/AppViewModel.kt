package net.xzos.upgradeall.ui.data.livedata

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import net.xzos.upgradeall.core.database.table.AppEntity
import net.xzos.upgradeall.core.manager.AppManager
import net.xzos.upgradeall.core.module.app.App
import net.xzos.upgradeall.core.utils.coroutines.coroutinesMutableMapOf
import net.xzos.upgradeall.core.utils.oberver.ObserverFun

open class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val appMap: MutableMap<App, Pair<ObserverFun<App>, ObserverFun<AppEntity>>> = coroutinesMutableMapOf(true)

    fun updateData() {
        appMap.forEach { entry ->
            entry.value.run {
                val app = entry.key
                first(app)
                second(app.appDatabase)
            }
        }
    }

    protected open fun updateData(app: App) {}

    fun addObserve(app: App, databaseObserver: ObserverFun<AppEntity>, versionDataObserver: ObserverFun<App>) {
        removeObserve(app)
        appMap[app] = Pair(versionDataObserver, databaseObserver)
        val appEntity = app.appDatabase
        AppManager.observeForever(AppManager.getAppChangedNotifyTag(appEntity), databaseObserver)
        AppManager.observeForever(AppManager.getAppUpdatedNotifyTag(appEntity), versionDataObserver)
    }

    fun removeObserve(app: App) {
        appMap.remove(app)
    }

    private fun clearObserve() {
        appMap.forEach {
            val pair = it.value
            AppManager.removeObserver(pair.first)
            AppManager.removeObserver(pair.second)
        }
    }

    override fun onCleared() {
        super.onCleared()
        clearObserve()
    }
}