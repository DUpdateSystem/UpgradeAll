package net.xzos.upgradeall.ui.detail.setting.attrlist

import net.xzos.upgradeall.databinding.ItemAppAttrSettingBinding
import net.xzos.upgradeall.ui.base.recycleview.RecyclerViewHolder

class AttrListHolder(private val binding: ItemAppAttrSettingBinding)
    : RecyclerViewHolder<AttrListItemView, AttrListHandler, ItemAppAttrSettingBinding>(binding, binding) {
    override fun doBind(itemView: AttrListItemView) {
        binding.item = itemView
    }

    override fun setHandler(handler: AttrListHandler) {
        binding.handler = handler
    }
}