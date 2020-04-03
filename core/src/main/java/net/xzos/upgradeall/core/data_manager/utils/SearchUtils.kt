package net.xzos.upgradeall.core.data_manager.utils

import org.apache.commons.text.similarity.FuzzyScore
import java.util.*

class SearchUtils(private val initSearchInfoList: List<SearchInfo>) {
    private val searchResultCacheMap = mutableMapOf<String, List<SearchInfo>>()

    fun search(searchString: String?): List<SearchInfo> {
        return if (searchString.isNullOrBlank()) listOf()
        else (getCacheResult(searchString)
            ?: iterateSearch(searchString))
            .sortedByDescending { it.matchInfo.getMaxPoint() }
    }

    private fun iterateSearch(searchString: String): List<SearchInfo> {
        return initSearchInfoList.toMutableList()
            .apply {
                this.map { searchInfo ->
                    val matchList = searchInfo.matchInfo.matchList
                    searchInfo.matchInfo.matchList =
                            StringMatchUtils.match(
                                    searchString,
                                    matchList.map { it.matchString }
                            )
                }
            }.filter { searchInfo ->
                searchInfo.matchInfo.matchList.isNotEmpty()
            }.also {
                if (it.isNotEmpty()) {
                    cacheResult(searchString, it)
                }
            }
    }

    private fun getCacheResult(searchString: String?): List<SearchInfo>? =
        searchResultCacheMap[searchString]

    private fun cacheResult(searchString: String?, searchInfoList: List<SearchInfo>) {
        if (!searchString.isNullOrBlank() && searchInfoList.isNotEmpty())
            searchResultCacheMap[searchString] = searchInfoList
    }

    class SearchInfo(
        val targetSort: CharSequence,
        val matchInfo: StringMatchUtils.MatchInfo
    ) {
        override fun toString(): String {
            return matchInfo.id
        }
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

    class MatchString(val matchString: CharSequence, val matchPoint: Int = DEFAULT_POINT) {
        companion object {
            const val DEFAULT_POINT = 0
        }
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
}
