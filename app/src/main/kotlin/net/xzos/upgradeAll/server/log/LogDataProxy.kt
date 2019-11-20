package net.xzos.upgradeAll.server.log

import androidx.lifecycle.LiveData
import net.xzos.upgradeAll.data.database.litepal.RepoDatabase
import org.json.JSONException
import org.json.JSONObject
import org.litepal.LitePal
import java.util.*

class LogDataProxy(val Log: LogUtil) {
    private val logJSONObject: JSONObject = Log.logJSONObject
    private val logLiveData: LogLiveData = Log.logLiveData

    internal val logSort: List<String>
        get() {
            val logSortList = ArrayList<String>()
            val it = logJSONObject.keys()
            while (it.hasNext()) {
                logSortList.add(it.next() as String)
            }
            return logSortList
        }

    val liveDataLogSortList: LiveData<List<String>>
        get() = logLiveData.sortList

    val logAllToString: String
        get() {
            val sortList = logSort
            val fullLogString = StringBuilder()
            for (logSort in sortList) {
                fullLogString.append(getLogStringBySort(logSort))
            }
            return fullLogString.toString()
        }

    internal fun getLogObjectId(logSort: String): List<String> {
        val logObjectId = ArrayList<String>()
        val logSortJson: JSONObject = try {
            logJSONObject.getJSONObject(logSort)
        } catch (e: JSONException) {
            JSONObject()
        }

        val it = logSortJson.keys()
        while (it.hasNext()) {
            logObjectId.add(it.next() as String)
        }
        return logObjectId
    }

    @Throws(JSONException::class)
    internal fun getLogMessageList(logObjectTag: Array<String>): List<String> {
        val logMessageArray = ArrayList<String>()
        val logSortString = logObjectTag[0]
        val logObjectId = logObjectTag[1]
        val logMessageJSONArray = logJSONObject.getJSONObject(logSortString).getJSONArray(logObjectId)
        val len = logMessageJSONArray.length()
        for (i in 0 until len) {
            logMessageArray.add(logMessageJSONArray.get(i).toString())
        }
        return logMessageArray
    }

    fun getLiveDataLogObjectIdList(logSort: String): LiveData<List<String>> {
        return logLiveData.getIdListInSort(logSort)
    }

    fun getLogMessageListLiveData(logObjectTag: Array<String>): LiveData<List<String>> {
        return logLiveData.getLogMassageList(logObjectTag)
    }

    fun clearLogAll() {
        Log.logJSONObject = JSONObject()
        logLiveData.setLogJSONObject(logJSONObject)
    }

    fun clearLogSort(logSort: String) {
        logJSONObject.remove(logSort)
        logLiveData.setLogJSONObject(logJSONObject)
    }

    fun clearLogMessage(logObjectTag: Array<String>) {
        val logSortString = logObjectTag[0]
        val logObjectId = logObjectTag[1]
        logJSONObject.getJSONObject(logSortString).remove(logObjectId)
        logLiveData.setLogJSONObject(logJSONObject)
    }

    fun getLogStringBySort(logSort: String): String {
        val fullLogString = StringBuilder(logSort + "\n")
        val sortDatabaseIdList = getLogObjectId(logSort)
        for (databaseIdString in sortDatabaseIdList) {
            var logString = getLogMessageToString(arrayOf(logSort, databaseIdString))
            logString = if (logString != null)
                logString.split("\n".toRegex(), 2).toTypedArray()[1]
            else
                ""
            fullLogString.append(logString).append("\n")
        }
        return fullLogString.toString()
    }

    private fun getLogMessageToString(logObjectTag: Array<String>): String? {
        val sort = logObjectTag[0]
        val name = "    " + getNameFromId(logObjectTag[1])!!
        val logMessageString = StringBuilder()
        val logMessageArray: List<String>
        try {
            logMessageArray = getLogMessageList(logObjectTag)
        } catch (e: JSONException) {
            return null
        }

        for (logMessage in logMessageArray)
            logMessageString.append("        ").append(logMessage).append("\n")
        return sort + "\n" + name + "\n" + logMessageString
    }

    companion object {

        fun getNameFromId(databaseIdString: String): String? {
            return try {
                val repoDatabase = LitePal.find(RepoDatabase::class.java, Integer.parseInt(databaseIdString).toLong())
                repoDatabase.name
            } catch (e: Throwable) {
                databaseIdString
            }
        }
    }
}
