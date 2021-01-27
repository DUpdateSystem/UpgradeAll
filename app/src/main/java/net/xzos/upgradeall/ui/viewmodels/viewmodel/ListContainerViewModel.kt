package net.xzos.upgradeall.ui.viewmodels.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import net.xzos.upgradeall.ui.viewmodels.view.ListView
import net.xzos.upgradeall.utils.mutableLiveDataOf
import net.xzos.upgradeall.utils.setValueBackground

abstract class ListContainerViewModel : ViewModel() {
    private val listLiveData: MutableLiveData<List<ListView>> by lazy {
        mutableLiveDataOf<List<ListView>>().also { loadData() }
    }

    abstract fun loadData()

    fun setList(list: List<ListView>) {
        listLiveData.setValueBackground(list)
    }

    fun getList(): LiveData<List<ListView>> = listLiveData
}