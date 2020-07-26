package net.xzos.upgradeall.ui.viewmodels.viewmodel

import net.xzos.upgradeall.core.data_manager.AppDatabaseManager
import net.xzos.upgradeall.core.oberver.Observer
import net.xzos.upgradeall.core.server_manager.module.applications.Applications
import net.xzos.upgradeall.data.AppUiDataManager
import net.xzos.upgradeall.data.gson.toItemListBean

class ApplicationsPageViewModel : AppListContainerViewModel() {

    lateinit var observe: Observer
    lateinit var applications: Applications

    internal fun setApplications(applications: Applications) {
        this.applications = applications
        setAppList(applications)
        observe = object : Observer {
            override fun onChanged(vars: Array<out Any>): Any? {
                return setAppList(applications)
            }
        }
        applications.observeForever(observe)
    }

    private fun setAppList(applications: Applications) {
        setAppList(applications.appList)
    }

    fun addItemToTabPage(position: Int, tabPageIndex: Int): Boolean {
        val databaseId = getAppList()[position].appDatabase.saveReturnId(true)
        if (databaseId == 0L) return false
        val database = AppDatabaseManager.getDatabase(databaseId) ?: return false
        return AppUiDataManager.addItem(database.toItemListBean(), tabPageIndex)
    }

    override fun onCleared() {
        super.onCleared()
        applications.removeObserver(observe)
    }
}
