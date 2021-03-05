package net.xzos.upgradeall.ui.applist.base.normal

import net.xzos.upgradeall.core.module.app.App
import net.xzos.upgradeall.ui.applist.base.AppHubListFragment
import net.xzos.upgradeall.ui.applist.base.TAB_ALL


open class NormalAppHubListFragment(appType: String, tabIndex: Int = TAB_ALL)
    : AppHubListFragment<NormalAppListItemView, NormalAppHubListViewHolder>(appType, tabIndex) {
    override val adapter = NormalAppHubListAdapter()
    override val listContainerViewConvertFun = fun(app: App) = NormalAppListItemView(app)
}