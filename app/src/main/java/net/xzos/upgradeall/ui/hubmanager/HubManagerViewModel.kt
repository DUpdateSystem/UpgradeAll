package net.xzos.upgradeall.ui.hubmanager

import android.app.Application
import net.xzos.upgradeall.core.data.json.HubConfigGson
import net.xzos.upgradeall.core.manager.CloudConfigGetter
import net.xzos.upgradeall.core.manager.HubManager
import net.xzos.upgradeall.ui.base.recycleview.ListContainerViewModel

class HubManagerViewModel(application: Application) : ListContainerViewModel<HubManagerListItemView>(application) {

    override suspend fun doLoadData(): List<HubManagerListItemView> {
        CloudConfigGetter.renew()
        return CloudConfigGetter.hubConfigList?.map { getCloudHubItemCardView(it) }
                ?: HubManager.getHubList().map {
                    getCloudHubItemCardView(it.hubConfig)
                }
    }

    companion object {
        private fun getCloudHubItemCardView(hubConfig: HubConfigGson): HubManagerListItemView {
            val name = hubConfig.info.hubName
            val uuid = hubConfig.uuid
            return HubManagerListItemView(name, uuid)
        }
    }
}