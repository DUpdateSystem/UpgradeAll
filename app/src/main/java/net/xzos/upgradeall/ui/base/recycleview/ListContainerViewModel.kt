package net.xzos.upgradeall.ui.base.recycleview

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.xzos.upgradeall.ui.base.recycleview.RecyclerViewAdapter.Companion.RENEW
import net.xzos.upgradeall.utils.mutableLiveDataOf
import net.xzos.upgradeall.utils.setValueBackground

abstract class ListContainerViewModel<T>(application: Application) : AndroidViewModel(application) {
    private val refresh = Mutex()
    private val listLiveData: MutableLiveData<Triple<List<T>, Int, String>> by lazy { mutableLiveDataOf() }

    fun loadData(list: List<T>? = null) {
        viewModelScope.launch {
            if (!refresh.isLocked) {
                refresh.withLock {
                    list?.apply {
                        renewList(list)
                    } ?: renewList(doLoadData())
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