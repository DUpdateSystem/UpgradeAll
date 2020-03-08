package net.xzos.upgradeall.server.log

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import net.xzos.dupdatesystem.core.data.json.nongson.ObjectTag
import net.xzos.dupdatesystem.core.log.LogDataProxy
import net.xzos.dupdatesystem.core.log.LogItemData
import net.xzos.upgradeall.utils.notifyObserver


object LogLiveData {

    private var logMap = hashMapOf<ObjectTag, MutableList<LogItemData>>()

    private val mLogMapLiveData = MutableLiveData(logMap)

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
