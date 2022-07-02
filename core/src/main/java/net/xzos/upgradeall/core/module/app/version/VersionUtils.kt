package net.xzos.upgradeall.core.module.app.version

import net.xzos.upgradeall.core.utils.log.Log
import net.xzos.upgradeall.core.utils.log.ObjectTag
import net.xzos.upgradeall.core.utils.log.ObjectTag.Companion.core
import net.xzos.upgradeall.core.utils.versioning.VersioningUtils

private const val TAG = "VersionUtils"
private val logObjectTag = ObjectTag(core, TAG)

fun getVersionNumberCharString(
    rawVersionNumber: String, invalidStringFieldRegexString: String?
): List<Pair<Char, Boolean>> {
    val preVersionNumberInfoList =
        rawVersionNumber.map { Pair(it, true) }.toMutableList()
    invalidStringFieldRegexString?.run {
        try {
            getInvalidVersionNumberIndex(rawVersionNumber, this).let {
                setVersionNumberInfoList(preVersionNumberInfoList, it)
            }
        } catch (e: Throwable) {
            Log.e(logObjectTag, TAG, " getKeyVersionNumber: ${e.stackTraceToString()}")
        }
    }

    val indexMap = mutableMapOf<Int, Int>()
    val preVersionNumber = StringBuilder()
    preVersionNumberInfoList.forEachIndexed { index, pair ->
        if (pair.second) {
            preVersionNumber.append(pair.first)
            indexMap[preVersionNumber.length - 1] = index
        }
    }

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
    return finishVersionNumberInfoList
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

fun getInvalidVersionNumberIndex(
    rawVersionNumber: String,
    invalidVersionNumberFieldRegexString: String
): List<Int> {
    val invalidVersionNumberFieldRegex = invalidVersionNumberFieldRegexString.toRegex()
    val list = invalidVersionNumberFieldRegex.findAll(rawVersionNumber)
    val indexList = mutableListOf<Int>()
    list.forEach {
        indexList.addAll(it.range.first..it.range.last)
    }
    return indexList
}