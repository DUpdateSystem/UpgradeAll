package net.xzos.upgradeall.ui.discover

import android.app.Application
import net.xzos.upgradeall.core.manager.CloudConfigGetter
import net.xzos.upgradeall.ui.base.recycleview.ListContainerViewModel
import net.xzos.upgradeall.websdk.data.json.AppConfigGson

class DiscoverViewModel(application: Application) :
    ListContainerViewModel<AppConfigGson>(application) {

    override suspend fun doLoadData(): List<AppConfigGson> {
        CloudConfigGetter.renew()
        return CloudConfigGetter.appConfigList ?: emptyList()
    }
}