package net.xzos.upgradeall.log

import net.xzos.upgradeall.data.json.nongson.ObjectTag
import org.apache.commons.text.StringEscapeUtils


object LogDataProxy {

    private val logMap = Log.logMap

    val logSort: MutableList<String>
        get() {
            val sorts = mutableListOf<String>()
            for (objectTag in logMap.keys) {
                if (!sorts.contains(objectTag.sort))
                    sorts.add(objectTag.sort)
            }
            return sorts
        }

    val logAllToString: String
        get() {
            val sortList = logSort
            val fullLogString = StringBuilder()
            for (logSort in sortList) {
                fullLogString.append(getLogStringBySort(logSort))
            }
            return fullLogString.toString()
        }

    fun getObjectTagBySort(logSort: String): List<ObjectTag> =
            logMap.keys.filter {
                it.sort == logSort
            }

    fun getLogMessageList(objectTag: ObjectTag): List<String> =
            logMap[objectTag]?.map {
                StringEscapeUtils.unescapeJava(it)
            } ?: listOf()

    fun clearLogAll() {
        logMap.clear()
        Log.notifyChange()
    }

    fun clearLogBySort(logSort: String) {
        val objectTagList = getObjectTagBySort(logSort)
        for (objectTag in objectTagList)
            logMap.remove(objectTag)
        if (objectTagList.isNotEmpty())
            Log.notifyChange()
    }

    fun clearLogByObjectTag(objectTag: ObjectTag) {
        logMap.remove(objectTag)?.run {
            Log.notifyChange()
        }
    }

    fun getLogStringBySort(logSort: String): String {
        val fullLogString = StringBuilder(logSort + "\n")
        val objectTagList = getObjectTagBySort(logSort)
        for (objectTag in objectTagList) {
            val logString = convertLogMessageToString(objectTag, false)
            fullLogString.append(logString).append("\n")
        }
        return fullLogString.toString()
    }

    private fun convertLogMessageToString(logObjectTag: ObjectTag, printSort: Boolean = true): String? {
        val sort = logObjectTag.sort
        val name = "    " + logObjectTag.name
        val logMessageString = StringBuilder()
        val logMessageArray: List<String> = getLogMessageList(logObjectTag)

        for (logMessage in logMessageArray)
            logMessageString.append("        ").append(logMessage).append("\n")
        return if (printSort)
            sort + "\n"
        else "" + name + "\n" + logMessageString
    }
}
