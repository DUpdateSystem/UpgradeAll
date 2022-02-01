package net.xzos.upgradeall.ui.base.selectlistdialog

import net.xzos.upgradeall.ui.base.databinding.EnableObservable
import net.xzos.upgradeall.ui.base.list.ListItemView

class SelectItem(
        val name: String,
        val id: String,
        enable: Boolean
) : ListItemView {
    val enableObservable: EnableObservable = EnableObservable(enable) {}
}