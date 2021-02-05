package net.xzos.upgradeall.ui.applist.base.update

import android.app.Application
import net.xzos.upgradeall.core.module.app.Updater
import net.xzos.upgradeall.ui.applist.base.AppHubListViewModel


class UpdateAppHubListViewModel(application: Application) : AppHubListViewModel<UpdateAppListItemView>(application) {
    override suspend fun doLoadData(): List<UpdateAppListItemView> {
        return getAppList().filter {
            it.getReleaseStatus() == Updater.APP_OUTDATED
        }.map { UpdateAppListItemView(it) }
    }
}