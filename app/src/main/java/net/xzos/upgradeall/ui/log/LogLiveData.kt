package net.xzos.upgradeall.ui.log

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import net.xzos.upgradeall.core.utils.coroutines.CoroutinesMutableList
import net.xzos.upgradeall.core.utils.coroutines.CoroutinesMutableMap
import net.xzos.upgradeall.core.utils.coroutines.coroutinesMutableMapOf
import net.xzos.upgradeall.core.utils.log.LogDataProxy
import net.xzos.upgradeall.core.utils.log.LogItemData
import net.xzos.upgradeall.core.utils.log.ObjectTag
import net.xzos.upgradeall.utils.notifyObserver


object LogLiveData {

    private var logMap = coroutinesMutableMapOf<ObjectTag, CoroutinesMutableList<LogItemData>>(true)

    private val mLogMapLiveData = MutableLiveData(logMap)

    fun notifyChange(logMap: CoroutinesMutableMap<ObjectTag, CoroutinesMutableList<LogItemData>>) {
        LogLiveData.logMap = logMap
        mLogMapLiveData.notifyObserver()
    }

    internal val sortList: LiveData<MutableList<String>>
        get() = mLogMapLiveData.map {
            LogDataProxy.logSort
        }

    fun getObjectTagListBySort(logSort: String): LiveData<List<ObjectTag>> {
        return mLogMapLiveData.map {
            LogDataProxy.getObjectTagBySort(logSort)
        }
    }

    fun getLogMassageList(objectTag: ObjectTag): LiveData<List<String>> {
        return mLogMapLiveData.map {
            LogDataProxy.getLogMessageList(objectTag)
        }
    }
}
