package net.xzos.upgradeall.ui.discover

import android.app.Application
import net.xzos.upgradeall.core.manager.CloudConfigGetter
import net.xzos.upgradeall.ui.base.recycleview.ListContainerViewModel
import net.xzos.upgradeall.ui.discover.DiscoverListItemView.Companion.getCloudAppItemCardView

class DiscoverViewModel(private val _application: Application) : ListContainerViewModel<DiscoverListItemView>(_application) {

    override suspend fun doLoadData(): List<DiscoverListItemView> {
        CloudConfigGetter.renew()
        return CloudConfigGetter.appConfigList?.mapNotNull { getCloudAppItemCardView(it, _application) }
                ?: emptyList()
    }
}