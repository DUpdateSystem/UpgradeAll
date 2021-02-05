package net.xzos.upgradeall.ui.applist.base.star

import android.app.Application
import net.xzos.upgradeall.core.data.json.uiConfig
import net.xzos.upgradeall.ui.applist.base.normal.NormalAppHubListViewModel
import net.xzos.upgradeall.ui.applist.base.normal.NormalAppListItemView


class StarAppHubListViewModel(application: Application) : NormalAppHubListViewModel(application) {
    override suspend fun doLoadData(): List<NormalAppListItemView> {
        val userStarAppIdList = uiConfig.user_star_app_id_list
        return super.doLoadData().filter { userStarAppIdList.contains(it.app.appId) }
    }
}