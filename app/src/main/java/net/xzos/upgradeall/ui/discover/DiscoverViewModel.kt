package net.xzos.upgradeall.ui.discover

import android.app.Application
import net.xzos.upgradeall.core.manager.CloudConfigGetter
import net.xzos.upgradeall.ui.base.recycleview.ListContainerViewModel
import net.xzos.upgradeall.getter.rpc.AppConfig

class DiscoverViewModel(application: Application) :
    ListContainerViewModel<AppConfig>(application) {

    override suspend fun doLoadData(): List<AppConfig> {
        CloudConfigGetter.renew()
        return CloudConfigGetter.appConfigList ?: emptyList()
    }
}