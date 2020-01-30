package net.xzos.upgradeAll.server.log

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import net.xzos.upgradeAll.data.json.nongson.ObjectTag


internal class LogLiveData(logMap: MutableMap<String, MutableMap<String, MutableList<String>>>) {

    internal val mLogMapLiveData = MutableLiveData(logMap)

    internal val sortList: LiveData<MutableSet<String>>
        get() = Transformations.map(mLogMapLiveData) {
            LogUtil.logDataProxy.logSort
        }

    fun getIdListInSort(logSort: String): LiveData<MutableSet<String>> {
        return Transformations.map(mLogMapLiveData) {
            LogUtil.logDataProxy.getLogObjectId(logSort)
        }
    }

    fun getLogMassageList(objectTag: ObjectTag): LiveData<List<String>> {
        return Transformations.map(mLogMapLiveData) {
            LogUtil.logDataProxy.getLogMessageList(objectTag)
        }
    }
}