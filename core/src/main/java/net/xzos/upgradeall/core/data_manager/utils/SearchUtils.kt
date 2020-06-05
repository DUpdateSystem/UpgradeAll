package net.xzos.upgradeall.core.data_manager.utils

import org.apache.commons.text.similarity.FuzzyScore
import java.util.*

class SearchUtils(private val initSearchInfoList: List<SearchInfo>) {
    private val searchResultCacheMap = mutableMapOf<String, List<SearchInfo>>()

    suspend fun search(searchString: String?): List<SearchInfo> {
        return if (searchString.isNullOrBlank()) listOf()
        else getCacheResult(searchString) ?: iterateSearch(searchString)
    }

    private fun iterateSearch(searchString: String): List<SearchInfo> {
        val searchInfoList: MutableList<SearchInfo> = mutableListOf()
        for (i in initSearchInfoList) {
            val searchInfo = i.copy()
            val matchList = StringMatchUtils.match(
                    searchString, searchInfo.matchInfo.matchList.map { it.matchString })
            if (matchList.isNotEmpty()) {
                searchInfo.matchInfo.matchList = matchList
                searchInfoList.add(searchInfo)
            }
        }
        return searchInfoList
                .sortedByDescending { it.matchInfo.getMaxPoint() }
                .also { if (it.isNotEmpty()) cacheResult(searchString, it) }
    }

    private fun getCacheResult(searchString: String?): List<SearchInfo>? =
            searchResultCacheMap[searchString]

    private fun cacheResult(searchString: String?, searchInfoList: List<SearchInfo>) {
        if (!searchString.isNullOrBlank() && searchInfoList.isNotEmpty())
            searchResultCacheMap[searchString] = searchInfoList
    }
}

object StringMatchUtils {

    fun match(
            searchString: CharSequence, matchStringList: List<CharSequence>,
            locale: Locale = Locale.getDefault()
    ): List<MatchString> {
        val matchList = mutableListOf<MatchString>()
        val fuzzyScore = FuzzyScore(locale)
        for (matchString in matchStringList) {
            val matchPoints = fuzzyScore.fuzzyScore(matchString, searchString)
            if (matchPoints > 0) {
                matchList.add(
                        MatchString(matchString, matchPoints)
                )
            }
        }
        return matchList
    }
}

data class SearchInfo(
        val targetSort: CharSequence,
        val matchInfo: MatchInfo
) {
    override fun toString(): String = matchInfo.id
}

class MatchInfo(val id: String, val name: String, var matchList: List<MatchString>) {
    fun getMaxPoint(): Int {
        var maxPoint = 0
        for (matchString in matchList) {
            if (matchString.matchPoint > maxPoint) {
                maxPoint = matchString.matchPoint
            }
        }
        return maxPoint
    }

}

class MatchString(val matchString: CharSequence, val matchPoint: Int = 0)
