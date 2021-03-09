package net.xzos.upgradeall.ui.applist.base.normal

import net.xzos.upgradeall.core.module.app.App
import net.xzos.upgradeall.ui.applist.base.AppHubListFragment


open class NormalAppHubListFragment
    : AppHubListFragment<NormalAppListItemView, NormalAppHubListViewHolder>() {
    override val adapter = NormalAppHubListAdapter()
    override val listContainerViewConvertFun = fun(app: App) = NormalAppListItemView(app)
}