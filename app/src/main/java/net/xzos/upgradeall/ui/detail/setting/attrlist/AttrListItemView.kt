package net.xzos.upgradeall.ui.detail.setting.attrlist

import androidx.databinding.ObservableField
import net.xzos.upgradeall.ui.base.list.ListItemView

class AttrListItemView(
        key:String, value:String
) : ListItemView{
    val key: ObservableField<String> = ObservableField<String>(key)
    val value: ObservableField<String> = ObservableField<String>(value)
}