package net.xzos.upgradeall.utils

import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * 拓展 LiveData 监听列表元素添加、删除操作的支持
 */
fun <T> MutableLiveData<T>.notifyObserver() {
    GlobalScope.launch(Dispatchers.Main) {
        this@notifyObserver.value = this@notifyObserver.value
    }
}
