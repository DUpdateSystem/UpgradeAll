package net.xzos.upgradeall.core.log

import net.xzos.upgradeall.core.data.json.nongson.ObjectTag


object LogDataProxy {

    private val logMap = Log.logMap

    private val allLogObjectTag = logMap.keys.toList()

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
                fullLogString.append(
                    getLogStringBySort(
                        logSort
                    )
                )
            }
            return fullLogString.toString()
        }

    fun getObjectTagBySort(logSort: String): List<ObjectTag> =
        logMap.keys.filter {
            it.sort == logSort
        }

    fun getLogMessageList(objectTag: ObjectTag): List<String> =
        logMap[objectTag]?.map {
            it.toString()
        } ?: listOf()

    fun getLogStringBySort(logSort: String): String {
        val fullLogString = StringBuilder(logSort + "\n")
        val objectTagList = getObjectTagBySort(logSort)
        for (objectTag in objectTagList) {
            val logString = convertLogMessageToString(
                objectTag,
                false
            )
            fullLogString.append(logString).append("\n")
        }
        return fullLogString.toString()
    }

    private fun convertLogMessageToString(logObjectTag: ObjectTag, printSort: Boolean = true): String? {
        val sort = logObjectTag.sort
        val name = "    " + logObjectTag.name
        val logMessageString = StringBuilder()
        val logMessageArray: List<String> =
            getLogMessageList(logObjectTag)

        for (logMessage in logMessageArray)
            logMessageString.append("        ").append(logMessage).append("\n")
        return if (printSort)
            sort + "\n"
        else "" + name + "\n" + logMessageString
    }

    fun clearLogAll() {
        logMap.clear()
        Log.notifyChange()
    }

    fun clearLogBySort(logSort: String) {
        val objectTagList = getObjectTagBySort(logSort)
        for (objectTag in objectTagList)
            logMap.remove(objectTag)
        Log.notifyChange()
    }

    fun clearLogByObjectTag(objectTag: ObjectTag) {
        logMap.remove(objectTag)?.run {
            Log.notifyChange()
        }
    }
}
