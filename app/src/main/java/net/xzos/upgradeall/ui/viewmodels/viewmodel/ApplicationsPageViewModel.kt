package net.xzos.upgradeall.ui.viewmodels.viewmodel

import net.xzos.upgradeall.core.oberver.Observer
import net.xzos.upgradeall.core.server_manager.module.applications.Applications
import net.xzos.upgradeall.utils.mutableLiveDataOf

class ApplicationsPageViewModel : AppListContainerViewModel() {

    init {
        appListLiveData = mutableLiveDataOf()
    }

    lateinit var observe: Observer
    lateinit var applications: Applications

    internal fun setApplications(applications: Applications) {
        this.applications = applications
        appListLiveData.value = applications.apps
        observe = object : Observer {
            override fun onChanged(vars: Array<out Any>): Any? {
                appListLiveData.value = applications.apps
                return null
            }
        }
        applications.observeForever(observe)
    }

    override fun onCleared() {
        super.onCleared()
        applications.removeObserver(observe)
    }
}
