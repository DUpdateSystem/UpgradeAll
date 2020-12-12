package net.xzos.upgradeall.ui.apphub.adapter

import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import net.xzos.upgradeall.R
import net.xzos.upgradeall.ui.viewmodels.view.CloudConfigListItemView

class DiscoveryAdapter : BaseQuickAdapter<CloudConfigListItemView, BaseViewHolder>(R.layout.item_hub_app) {

    override fun convert(holder: BaseViewHolder, item: CloudConfigListItemView) {
        holder.setText(R.id.tv_app_name, item.name)
        holder.setText(R.id.tv_package_name, item.hubName)
    }
}