package net.xzos.upgradeall.ui.discover

import androidx.activity.viewModels
import net.xzos.upgradeall.core.websdk.json.AppConfigGson
import net.xzos.upgradeall.ui.base.list.HubListActivity


class DiscoverActivity :
    HubListActivity<AppConfigGson, DiscoverListItemView, DiscoverListViewHolder>({ it.info.name }) {

    override val viewModel by viewModels<DiscoverViewModel>()
    override val adapter by lazy {
        DiscoveryAdapter(
            DiscoverListItemHandler(
                viewModel,
                supportFragmentManager
            )
        ).apply { setHasStableIds(true) }
    }
}