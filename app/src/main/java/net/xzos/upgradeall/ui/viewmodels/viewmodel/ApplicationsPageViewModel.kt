package net.xzos.upgradeall.ui.viewmodels.viewmodel

import kotlinx.coroutines.runBlocking
import net.xzos.upgradeall.core.data.database.AppDatabase
import net.xzos.upgradeall.core.data_manager.AppDatabaseManager
import net.xzos.upgradeall.core.oberver.ObserverFun
import net.xzos.upgradeall.core.server_manager.module.applications.Applications
import net.xzos.upgradeall.data.AppUiDataManager
import net.xzos.upgradeall.data.gson.toItemListBean

class ApplicationsPageViewModel : AppListContainerViewModel() {

    private lateinit var observe: ObserverFun<Unit>
    lateinit var applications: Applications

    internal fun setApplications(applications: Applications) {
        this.applications = applications
        setAppList(applications)
        observe = fun(_) { setAppList(applications) }
        applications.observeForever(observe)
    }

    private fun setAppList(applications: Applications) {
        setAppList(applications.appList)
    }

    fun addItemToTabPage(position: Int, tabPageIndex: Int): Boolean {
        val database = getAppList()[position].appDatabase as AppDatabase
        val databaseId = runBlocking { AppDatabaseManager.insertAppDatabase(database) }
        if (databaseId == 0L) return false
        return AppUiDataManager.addItem(database.toItemListBean(), tabPageIndex)
    }

    override fun onCleared() {
        super.onCleared()
        applications.removeObserver(observe)
    }
}
