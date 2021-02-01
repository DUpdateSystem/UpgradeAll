package net.xzos.upgradeall.ui.apphub.discover

import androidx.activity.viewModels
import net.xzos.upgradeall.ui.apphub.HubListActivity
import net.xzos.upgradeall.ui.apphub.adapter.DiscoveryAdapter
import net.xzos.upgradeall.ui.viewmodels.view.CloudConfigListItemView
import net.xzos.upgradeall.ui.viewmodels.view.holder.DiscoverListViewHolder
import net.xzos.upgradeall.ui.viewmodels.viewmodel.DiscoveryViewModel


class DiscoverActivity : HubListActivity<CloudConfigListItemView, DiscoverListViewHolder>() {

    override val viewModel by viewModels<DiscoveryViewModel>()
    override val adapter = DiscoveryAdapter()

    override fun initView() {
        super.initView()
        adapter.apply {
            setOnItemClickListener { _, position ->
                getItemData(position).uuid.run {
                    ConfigDownloadDialog(this, viewModel).show(supportFragmentManager)
                }
            }
        }
    }
}