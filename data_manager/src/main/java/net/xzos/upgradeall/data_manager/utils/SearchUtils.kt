package net.xzos.upgradeall.data_manager.utils

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.apache.commons.text.similarity.FuzzyScore
import java.util.*

class SearchUtils(private val initSearchInfoList: List<SearchInfo>) {
    private val mutex = Mutex()

    private val searchResultCacheList = mutableListOf<Pair<String?, List<SearchInfo>>>()

    suspend fun search(searchString: String?): List<SearchInfo> {
        return mutex.withLock {
            if (searchString.isNullOrBlank())
                listOf()
            else
                getCacheResult(searchString)
                        ?: iterateSearch(searchString)
        }
    }

    private fun iterateSearch(searchString: String): List<SearchInfo> {
        return getCacheResult(searchString.dropLast(1))?.toTypedArray()?.copyOf()?.toMutableList()
                ?.apply {
                    this.map { searchInfo ->
                        val matchList = searchInfo.matchInfo.matchList
                        searchInfo.matchInfo.matchList = StringMatchUtils.match(
                                searchString,
                                matchList.map { it.matchString }
                        )
                    }
                }?.filter { searchInfo ->
                    searchInfo.matchInfo.matchList.isNotEmpty()
                }?.also {
                    cacheResult(searchString, it)
                } ?: initSearchInfoList  // 使用初始数据
    }

    private fun getCacheResult(searchString: String?): List<SearchInfo>? {
        if (searchResultCacheList.map { it.first }.contains(searchString)) {
            for (searchResult in searchResultCacheList) {
                if (searchResult.first == searchString) {
                    searchResultCacheList.remove(searchResult)
                    searchResultCacheList.add(0, searchResult)
                    return searchResult.second
                }
            }
        }
        return null
    }

    private fun cacheResult(searchString: String?, searchInfoList: List<SearchInfo>) {
        if (!searchString.isNullOrBlank() && searchInfoList.isNotEmpty())
            searchResultCacheList.add(0, Pair(searchString, searchInfoList))
        if (searchResultCacheList.size > searchResultCacheListSize)
            searchResultCacheList.dropLast(searchResultCacheList.size - searchResultCacheListSize)
    }

    fun clearResultCache() {
        searchResultCacheList.clear()
    }

    data class SearchInfo(
            val targetSort: CharSequence,
            val matchInfo: StringMatchUtils.MatchInfo) {
        override fun toString(): String {
            return matchInfo.id
        }
    }

    companion object {
        private const val searchResultCacheListSize = 8
    }
}

object StringMatchUtils {

    fun match(searchString: CharSequence?, matchStringList: List<CharSequence>,
              locale: Locale = Locale.getDefault()
    ): List<MatchString> {
        val matchList = mutableListOf<MatchString>()
        if (searchString != null) {
            val fuzzyScore = FuzzyScore(locale)
            for (matchString in matchStringList) {
                val matchPoints = fuzzyScore.fuzzyScore(matchString, searchString)
                if (matchPoints > 0) {
                    matchList.add(MatchString(matchString, matchPoints))
                }
            }
        }
        return matchList
    }

    data class MatchString(val matchString: CharSequence, val matchPoint: Int)

    data class MatchInfo(val id: String, val name: String, var matchList: List<MatchString>) {
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
