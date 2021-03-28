package net.xzos.upgradeall.ui.detail.setting.attrlist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import net.xzos.upgradeall.ui.base.recycleview.RecyclerViewAdapter.Companion.ADD
import net.xzos.upgradeall.ui.base.recycleview.RecyclerViewAdapter.Companion.DEL

class AttrListViewModel(application: Application) : AndroidViewModel(application) {
    lateinit var adapter: AttrListAdapter

    fun addEmptyItem() {
        addItem("", null)
    }

    fun addItem(key: String, value: String?) {
        val attrListItemView = AttrListItemView(key, value ?: "")
        val dataSet = adapter.getAdapterData().toMutableList().apply {
            add(attrListItemView)
        }
        adapter.setAdapterData(dataSet, dataSet.size - 1, ADD)
    }

    fun delItem(item: AttrListItemView) {
        val dataSet = adapter.getAdapterData().toMutableList()
        val position = dataSet.indexOf(item)
        dataSet.removeAt(position)
        adapter.setAdapterData(dataSet, position, DEL)
    }
}