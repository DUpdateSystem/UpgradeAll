package net.xzos.upgradeall.ui.viewmodels.viewmodel

import androidx.lifecycle.MutableLiveData
import net.xzos.dupdatesystem.core.server_manager.module.applications.Applications

class ApplicationsPageViewModel : AppListContainerViewModel() {

    private val applications = MutableLiveData<Applications>().apply {
        this.observeForever { applications ->
            val needUpdateAppList = applications.getNeedUpdateAppList(false)
            val appList = needUpdateAppList + applications.apps.filter {
                !needUpdateAppList.contains(it)
            }
            setApps(appList)
        }
    }

    internal fun setApplications(applications: Applications) {
        this.applications.value = applications
    }
}
