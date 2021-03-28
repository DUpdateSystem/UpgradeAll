package net.xzos.upgradeall.ui.discover

import android.app.Application
import net.xzos.upgradeall.core.manager.CloudConfigGetter
import net.xzos.upgradeall.ui.base.recycleview.ListContainerViewModel
import net.xzos.upgradeall.ui.discover.DiscoverListItemView.Companion.getCloudAppItemCardView

class DiscoverViewModel(application: Application) : ListContainerViewModel<DiscoverListItemView>(application) {

    override suspend fun doLoadData(): List<DiscoverListItemView> {
        CloudConfigGetter.renew()
        return CloudConfigGetter.appConfigList?.mapNotNull { getCloudAppItemCardView(it, getApplication()) }
                ?: emptyList()
    }
}