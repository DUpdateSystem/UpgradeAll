package net.xzos.upgradeAll.server.log

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import net.xzos.upgradeAll.server.ServerContainer
import org.json.JSONObject

internal class LogLiveData {
    private val mLogJSONObject = MutableLiveData(JSONObject())

    fun setLogJSONObject(logJSONObject: JSONObject) {
        mLogJSONObject.postValue(logJSONObject)
    }

    val sortList: LiveData<List<String>>
        get() = Transformations.map(mLogJSONObject) {
            val logUtil = ServerContainer.Log
            val logDataProxy = LogDataProxy(logUtil)
            logDataProxy.logSort
        }

    fun getIdListInSort(logSort: String): LiveData<List<String>> {
        return Transformations.map(mLogJSONObject) {
            val logUtil = ServerContainer.Log
            val logDataProxy = LogDataProxy(logUtil)
            logDataProxy.getLogObjectId(logSort)
        }
    }

    fun getLogMassageList(logObjectTag: Array<String>): LiveData<List<String>> {
        return Transformations.map(mLogJSONObject) {
            val logUtil = ServerContainer.Log
            val logDataProxy = LogDataProxy(logUtil)
            logDataProxy.getLogMessageList(logObjectTag)
        }
    }
}
