package net.xzos.upgradeall.data.log

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import net.xzos.upgradeall.core.log.LogDataProxy
import net.xzos.upgradeall.core.log.LogItemData
import net.xzos.upgradeall.core.log.ObjectTag
import net.xzos.upgradeall.core.utils.coroutines.CoroutinesMutableList
import net.xzos.upgradeall.core.utils.coroutines.CoroutinesMutableMap
import net.xzos.upgradeall.core.utils.coroutines.coroutinesMutableMapOf
import net.xzos.upgradeall.utils.notifyObserver


object LogLiveData {

    private var logMap = coroutinesMutableMapOf<ObjectTag, CoroutinesMutableList<LogItemData>>(true)

    private val mLogMapLiveData = MutableLiveData(logMap)

    fun notifyChange(logMap: CoroutinesMutableMap<ObjectTag, CoroutinesMutableList<LogItemData>>) {
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
