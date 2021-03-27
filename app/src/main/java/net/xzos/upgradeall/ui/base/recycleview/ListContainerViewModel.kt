package net.xzos.upgradeall.ui.base.recycleview

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import net.xzos.upgradeall.core.utils.runWithLock
import net.xzos.upgradeall.ui.base.recycleview.RecyclerViewAdapter.Companion.RENEW
import net.xzos.upgradeall.utils.mutableLiveDataOf
import net.xzos.upgradeall.utils.setValueBackground

abstract class ListContainerViewModel<T>(application: Application) : AndroidViewModel(application) {
    private var refresh = Mutex()
    private val listLiveData: MutableLiveData<Triple<List<T>, Int, String>> by lazy { mutableLiveDataOf() }

    fun loadData() {
        if (!refresh.isLocked) {
            refresh.runWithLock {
                viewModelScope.launch(Dispatchers.IO) {
                    renewList(doLoadData())
                }
            }
        }
    }

    abstract suspend fun doLoadData(): List<T>

    fun renewList(list: List<T>) {
        setList(list, -1, RENEW)
    }

    fun setList(list: List<T>, changedPosition: Int, changedTag: String) {
        listLiveData.setValueBackground(Triple(list, changedPosition, changedTag))
    }

    fun getLiveData(): LiveData<Triple<List<T>, Int, String>> = listLiveData
}