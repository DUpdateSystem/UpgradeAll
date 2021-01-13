package net.xzos.upgradeall.ui.apphub.adapter

import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import net.xzos.upgradeall.R
import net.xzos.upgradeall.ui.viewmodels.view.ItemCardView

class HubListAdapter : BaseQuickAdapter<ItemCardView, BaseViewHolder>(R.layout.item_hub_app) {

    override fun convert(holder: BaseViewHolder, item: ItemCardView) {
        holder.setText(R.id.tv_app_name, item.name)
    }
}