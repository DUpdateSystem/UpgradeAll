package net.xzos.upgradeall.ui.hubmanager

import androidx.activity.viewModels
import net.xzos.upgradeall.ui.base.list.HubListActivity


class HubManagerActivity : HubListActivity<HubManagerListItemView, HubManagerListViewHolder>() {

    override val viewModel by viewModels<HubManagerViewModel>()
    override val adapter by lazy { HubManagerAdapter(HubManagerListItemHandler()) }
}