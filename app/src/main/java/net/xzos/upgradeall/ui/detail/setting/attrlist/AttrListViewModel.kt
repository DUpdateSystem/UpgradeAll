package net.xzos.upgradeall.ui.detail.setting.attrlist

import android.app.Application
import androidx.lifecycle.AndroidViewModel

class AttrListViewModel(application: Application) : AndroidViewModel(application) {
    lateinit var adapter: AttrListAdapter

    fun addEmptyItem() {
        addItem("", null)
    }

    fun addItem(key: String, value: String?) {
        val attrListItemView = AttrListItemView(key, value ?: "")
        adapter.dataSet = adapter.dataSet.toMutableList().apply {
            add(attrListItemView)
        }
    }

    fun delItem(item: AttrListItemView) {
        val dataSet = adapter.dataSet.toMutableList()
        val position = dataSet.indexOf(item)
        dataSet.removeAt(position)
        adapter.dataSet = dataSet
    }
}