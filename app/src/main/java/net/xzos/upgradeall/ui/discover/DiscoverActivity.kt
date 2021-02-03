package net.xzos.upgradeall.ui.discover

import androidx.activity.viewModels
import net.xzos.upgradeall.ui.base.list.HubListActivity
import net.xzos.upgradeall.utils.runUiFun


class DiscoverActivity : HubListActivity<DiscoverListItemView, DiscoverListViewHolder>() {

    override val viewModel by viewModels<DiscoverViewModel>()
    override val adapter = DiscoveryAdapter()

    override fun initView() {
        super.initView()
        adapter.apply {
            setOnItemClickListener { _, position ->
                getItemData(position).uuid.run {
                    ConfigDownloadDialog(this) {
                        runUiFun { viewModel.loadData() }
                    }.show(supportFragmentManager)
                }
            }
        }
    }
}