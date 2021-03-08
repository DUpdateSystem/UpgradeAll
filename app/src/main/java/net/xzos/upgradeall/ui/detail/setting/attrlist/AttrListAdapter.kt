package net.xzos.upgradeall.ui.detail.setting.attrlist

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.lifecycle.LifecycleCoroutineScope
import net.xzos.upgradeall.core.utils.getAllLocalKeyList
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
        binding.keyEdit.run {
            val array = getAllLocalKeyList()
            val adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, array)

            setAdapter(adapter)
        }
        return AttrListHolder(binding)
    }
}