package net.xzos.upgradeall.ui.home.adapter

import com.chad.library.adapter.base.BaseMultiItemQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import net.xzos.upgradeall.R

class HomeModuleAdapter : BaseMultiItemQuickAdapter<HomeModuleBean, BaseViewHolder>() {

    init {
        addItemType(STYLE_CARD, R.layout.item_home_module_card)
        addItemType(STYLE_NON_CARD, R.layout.item_home_module_non_card)
        addItemType(STYLE_SIMPLE_CARD, R.layout.item_home_module_non_card)
    }

    override fun convert(holder: BaseViewHolder, item: HomeModuleBean) {
        holder.setImageResource(R.id.iv_icon, item.iconRes)
        holder.setText(R.id.tv_title, item.titleRes)
        holder.itemView.setOnClickListener(item.clickListener)
    }
}