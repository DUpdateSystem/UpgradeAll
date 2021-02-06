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
import net.xzos.upgradeall.utils.mutableLiveDataOf
import net.xzos.upgradeall.utils.setValueBackground

abstract class ListContainerViewModel<T>(application: Application) : AndroidViewModel(application) {
    private var refresh = Mutex()
    private val listLiveData: MutableLiveData<List<T>> by lazy {
        mutableLiveDataOf<List<T>>().also {
            loadData()
        }
    }

    fun loadData() {
        if (!refresh.isLocked) {
            refresh.runWithLock {
                viewModelScope.launch(Dispatchers.IO) {
                    setList(doLoadData())
                }
            }
        }
    }

    abstract suspend fun doLoadData(): List<T>

    private fun setList(list: List<T>) {
        listLiveData.setValueBackground(list)
    }

    fun getList(): LiveData<List<T>> = listLiveData
}