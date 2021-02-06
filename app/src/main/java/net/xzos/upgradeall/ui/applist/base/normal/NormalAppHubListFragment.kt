package net.xzos.upgradeall.ui.applist.base.normal

import net.xzos.upgradeall.core.module.app.App
import net.xzos.upgradeall.ui.applist.base.AppHubListFragment
import net.xzos.upgradeall.ui.applist.base.AppHubViewModel


open class NormalAppHubListFragment(viewModel: AppHubViewModel, tabIndex: Int = NORMAL_TAB)
    : AppHubListFragment<NormalAppListItemView, NormalAppHubListViewHolder>(viewModel, tabIndex) {
    override val adapter = NormalAppHubListAdapter()
    override val listContainerViewConvertFun = fun(app: App) = NormalAppListItemView(app)
}