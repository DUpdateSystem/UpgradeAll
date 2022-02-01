package net.xzos.upgradeall.ui.hubmanager

import android.app.Application
import net.xzos.upgradeall.core.manager.CloudConfigGetter
import net.xzos.upgradeall.core.manager.HubManager
import net.xzos.upgradeall.ui.base.recycleview.ListContainerViewModel
import net.xzos.upgradeall.ui.hubmanager.HubManagerListItemView.Companion.getCloudHubItemCardView

class HubManagerViewModel(application: Application) : ListContainerViewModel<HubManagerListItemView>(application) {

    override suspend fun doLoadData(): List<HubManagerListItemView> {
        CloudConfigGetter.renew()
        val hubConfigList = CloudConfigGetter.hubConfigList
                ?: HubManager.getHubList().map { it.hubConfig }
        return hubConfigList.map { getCloudHubItemCardView(it, getApplication()) }
    }
}