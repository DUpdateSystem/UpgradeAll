package net.xzos.upgradeall.ui.hubmanager

import android.app.Application
import net.xzos.upgradeall.core.manager.CloudConfigGetter
import net.xzos.upgradeall.core.manager.HubManager
import net.xzos.upgradeall.getter.rpc.HubConfig
import net.xzos.upgradeall.getter.rpc.HubConfigInfo
import net.xzos.upgradeall.ui.base.recycleview.ListContainerViewModel
import net.xzos.upgradeall.ui.hubmanager.HubManagerListItemView.Companion.getCloudHubItemCardView

class HubManagerViewModel(application: Application) : ListContainerViewModel<HubManagerListItemView>(application) {

    override suspend fun doLoadData(): List<HubManagerListItemView> {
        CloudConfigGetter.renew()
        val hubConfigList: List<HubConfig> = CloudConfigGetter.hubConfigList
            ?: HubManager.getHubList().map { hub ->
                // Convert legacy HubConfigGson to the new HubConfig type for display
                val cfg = hub.hubConfig
                HubConfig(
                    baseVersion = cfg.baseVersion,
                    configVersion = cfg.configVersion,
                    uuid = cfg.uuid,
                    info = HubConfigInfo(hubName = cfg.info.hubName, hubIconUrl = cfg.info.hubIconUrl),
                    apiKeywords = cfg.apiKeywords,
                    appUrlTemplates = cfg.appUrlTemplates,
                    targetCheckApi = cfg.targetCheckApi,
                )
            }
        return hubConfigList.map { getCloudHubItemCardView(it, getApplication()) }
    }
}