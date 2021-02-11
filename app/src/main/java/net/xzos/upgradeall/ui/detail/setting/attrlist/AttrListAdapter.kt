package net.xzos.upgradeall.ui.detail.setting.attrlist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleCoroutineScope
import net.xzos.upgradeall.databinding.ItemAppAttrSettingBinding
import net.xzos.upgradeall.ui.base.recycleview.RecyclerViewAdapter

class AttrListAdapter(
        lifecycleScope: LifecycleCoroutineScope,
        viewModel: AttrListViewModel,
) : RecyclerViewAdapter<AttrListItemView, AttrListHandler, AttrListHolder>() {
    override val handler: AttrListHandler = AttrListHandler(viewModel)

    init {
        this.lifecycleScope = lifecycleScope
    }

    override fun getViewHolder(layoutInflater: LayoutInflater, viewGroup: ViewGroup): AttrListHolder {
        val binding = ItemAppAttrSettingBinding.inflate(layoutInflater, viewGroup, false)
        return AttrListHolder(binding)
    }
}