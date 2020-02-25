package net.xzos.upgradeall.server.log

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import net.xzos.dupdatesystem.data.json.nongson.ObjectTag
import net.xzos.dupdatesystem.log.Log
import net.xzos.dupdatesystem.log.LogDataProxy


object LogLiveData {

    private var logMap: MutableMap<ObjectTag, MutableList<String>> = Log.logMap

    fun notifyChange() {
        mLogMapLiveData.notifyObserver()
    }

    private fun <T> MutableLiveData<T>.notifyObserver() {
        runBlocking(Dispatchers.Main) {
            this@notifyObserver.value = this@notifyObserver.value
        }
    }

    internal val mLogMapLiveData = MutableLiveData(logMap)

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
