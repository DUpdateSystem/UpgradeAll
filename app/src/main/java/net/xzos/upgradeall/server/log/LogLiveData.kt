package net.xzos.upgradeall.server.log

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import net.xzos.dupdatesystem.core.data.json.nongson.ObjectTag
import net.xzos.dupdatesystem.core.log.LogDataProxy
import net.xzos.dupdatesystem.core.log.LogItemData


object LogLiveData {

    private var logMap = hashMapOf<ObjectTag, MutableList<LogItemData>>()

    private val mLogMapLiveData = MutableLiveData(logMap)

    private fun <T> MutableLiveData<T>.notifyObserver() {
        runBlocking(Dispatchers.Main) {
            this@notifyObserver.value = this@notifyObserver.value
        }
    }

    fun notifyChange(logMap: HashMap<ObjectTag, MutableList<LogItemData>>) {
        this.logMap = logMap
        mLogMapLiveData.notifyObserver()
    }

    internal val sortList: LiveData<MutableList<String>>
        get() = Transformations.map(mLogMapLiveData) {
            LogDataProxy.logSort
        }

    fun getObjectTagListBySort(logSort: String): LiveData<List<ObjectTag>> {
        return Transformations.map(mLogMapLiveData) {
            LogDataProxy.getObjectTagBySort(logSort)
        }
    }

    fun getLogMassageList(objectTag: ObjectTag): LiveData<List<String>> {
        return Transformations.map(mLogMapLiveData) {
            LogDataProxy.getLogMessageList(objectTag)
        }
    }
}
