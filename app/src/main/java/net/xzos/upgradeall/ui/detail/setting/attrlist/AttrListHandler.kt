package net.xzos.upgradeall.ui.detail.setting.attrlist

import net.xzos.upgradeall.ui.base.recycleview.RecyclerViewHandler

class AttrListHandler(private val viewModel: AttrListViewModel) : RecyclerViewHandler() {
    fun deleteItem(view: AttrListItemView) {
        viewModel.delItem(view)
    }
}