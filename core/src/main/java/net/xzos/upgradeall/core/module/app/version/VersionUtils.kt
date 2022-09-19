package net.xzos.upgradeall.core.module.app.version

import net.xzos.upgradeall.core.utils.log.Log
import net.xzos.upgradeall.core.utils.log.ObjectTag
import net.xzos.upgradeall.core.utils.log.ObjectTag.Companion.core
import net.xzos.upgradeall.core.utils.versioning.VersioningUtils

private const val TAG = "VersionUtils"
private val logObjectTag = ObjectTag(core, TAG)

fun getVersionNumberCharString(
    rawVersionNumber: String,
    invalidStringFieldRegexString: String?,
    includeVersionNumberRegex: String?,
): List<Pair<Char, Boolean>> {
    // pre clean version by custom regex
    val preVersionNumberInfoList =
        rawVersionNumber.map { Pair(it, true) }.toMutableList()
    invalidStringFieldRegexString?.run {
        try {
            setVersionNumberInfoList(
                preVersionNumberInfoList,
                getMatchIndex(rawVersionNumber, this)
            )
        } catch (e: Throwable) {
            Log.e(logObjectTag, TAG, " getKeyVersionNumber: ${e.stackTraceToString()}")
        }
    }
    // get the map of index of pre clean list and raw list
    val indexMap = mutableMapOf<Int, Int>()
    val preVersionNumber = StringBuilder()
    preVersionNumberInfoList.forEachIndexed { index, pair ->
        if (pair.second) {
            preVersionNumber.append(pair.first)
            indexMap[preVersionNumber.length - 1] = index
        }
    }
    // get version number
    val finishVersionNumberInfoList =
        rawVersionNumber.map { Pair(it, false) }.toMutableList()
    VersioningUtils.matchVersioningString(preVersionNumber)?.range?.run {
        val versionNumberIndexList = mutableListOf<Int>()
        for (index in this.first..this.last) {
            versionNumberIndexList.add(indexMap[index]!!)
        }
        setVersionNumberInfoList(
            finishVersionNumberInfoList,
            versionNumberIndexList,
            true
        )
    }
    // include string in version number by custom regex
    val includeVersionNumberInfoList =
        rawVersionNumber.map { Pair(it, false) }.toMutableList()
    includeVersionNumberRegex?.run {
        try {
            setVersionNumberInfoList(
                includeVersionNumberInfoList,
                getMatchIndex(rawVersionNumber, this), true
            )
        } catch (e: Throwable) {
            Log.e(logObjectTag, TAG, " getKeyVersionNumber: ${e.stackTraceToString()}")
        }
    }
    return finishVersionNumberInfoList.mapIndexed { index, pair ->
        Pair(pair.first, pair.second || includeVersionNumberInfoList[index].second)
    }
}

private fun setVersionNumberInfoList(
    versionNumberInfoList: MutableList<Pair<Char, Boolean>>,
    versionNumberIndexList: List<Int>, enable: Boolean = false
) {
    versionNumberIndexList.forEach {
        val newValue = versionNumberInfoList[it].copy(second = enable)
        versionNumberInfoList.removeAt(it)
        versionNumberInfoList.add(it, newValue)
    }
}

fun getMatchIndex(
    s: String,
    regexString: String
): List<Int> {
    val invalidVersionNumberFieldRegex = regexString.toRegex()
    val list = invalidVersionNumberFieldRegex.findAll(s)
    val indexList = mutableListOf<Int>()
    list.forEach {
        indexList.addAll(it.range.first..it.range.last)
    }
    return indexList
}