package net.xzos.upgradeall.ui.applist.base.normal

import android.app.Application
import net.xzos.upgradeall.ui.applist.base.AppHubListViewModel


open class NormalAppHubListViewModel(application: Application) : AppHubListViewModel<NormalAppListItemView>(application) {
    override suspend fun doLoadData(): List<NormalAppListItemView> {
        return getAppList().map { NormalAppListItemView(it) }
    }
}