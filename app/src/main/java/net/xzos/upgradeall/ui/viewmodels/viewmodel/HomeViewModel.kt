package net.xzos.upgradeall.ui.viewmodels.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class HomeViewModel(application: Application) : AndroidViewModel(application) {
    val needUpdateCountLiveData: MutableLiveData<Int> = MutableLiveData(0)

    fun checkUpdates() = viewModelScope.launch(Dispatchers.IO) {
        delay(2000)

        withContext(Dispatchers.Main) {
            needUpdateCountLiveData.value = (1..10).random()
        }
    }
}