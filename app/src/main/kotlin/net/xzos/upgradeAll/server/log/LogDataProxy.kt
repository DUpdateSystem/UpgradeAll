package net.xzos.upgradeAll.server.log

import androidx.lifecycle.LiveData
import net.xzos.upgradeAll.data.database.manager.AppDatabaseManager
import net.xzos.upgradeAll.server.ServerContainer.Companion.Log
import org.json.JSONException

internal class LogDataProxy(private val logMap: MutableMap<String, MutableMap<String, MutableList<String>>>) {
    private val logLiveData = LogUtil.logLiveData

    internal val logSort: MutableSet<String>
        get() = logMap.keys

    val liveDataLogSortList: LiveData<MutableSet<String>>
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

    internal fun getLogObjectId(logSort: String): MutableSet<String> =
            this.logMap[logSort]?.keys ?: mutableSetOf()

    internal fun getLogMessageList(logObjectTag: Pair<String, String>): List<String> {
        val logSortString = logObjectTag.first
        val logObjectIdString = logObjectTag.second
        return logMap[logSortString]?.get(logObjectIdString) ?: listOf()
    }

    fun getLiveDataLogObjectIdList(logSort: String): LiveData<MutableSet<String>> {
        return logLiveData.getIdListInSort(logSort)
    }

    fun getLogMessageListLiveData(logObjectTag: Pair<String, String>): LiveData<List<String>> {
        return logLiveData.getLogMassageList(logObjectTag)
    }

    fun clearLogAll() {
        logMap.clear()
        Log.notifyObserver()
    }

    fun clearLogSort(logSort: String) {
        logMap.remove(logSort)
        Log.notifyObserver()
    }

    fun clearLogMessage(logObjectTag: Pair<String, String>) {
        val logSortString = logObjectTag.first
        val logObjectId = logObjectTag.second
        this.logMap[logSortString]?.remove(logObjectId)
        Log.notifyObserver()
    }

    fun getLogStringBySort(logSort: String): String {
        val fullLogString = StringBuilder(logSort + "\n")
        val sortDatabaseIdList = getLogObjectId(logSort)
        for (databaseIdString in sortDatabaseIdList) {
            var logString = getLogMessageToString(Pair(logSort, databaseIdString))
            logString = if (logString != null)
                logString.split("\n".toRegex(), 2).toTypedArray()[1]
            else
                ""
            fullLogString.append(logString).append("\n")
        }
        return fullLogString.toString()
    }

    private fun getLogMessageToString(logObjectTag: Pair<String, String>): String? {
        val sort = logObjectTag.first
        val name = "    " + getNameFromId(logObjectTag.second)
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

    internal fun getNameFromId(databaseIdString: String): String? = try {
        AppDatabaseManager.getDatabase(Integer.parseInt(databaseIdString).toLong())?.name
    } catch (e: Throwable) {
        databaseIdString
    }
}