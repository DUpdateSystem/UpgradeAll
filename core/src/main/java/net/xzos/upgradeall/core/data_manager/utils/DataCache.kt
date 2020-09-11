package net.xzos.upgradeall.core.data_manager.utils

import net.xzos.upgradeall.core.data.config.AppValue
import net.xzos.upgradeall.core.route.ReleaseListItem
import java.util.*


object DataCache {

    private val cache = Cache()

    private var dataExpirationTime = AppValue.data_expiration_time

    private fun Pair<Any?, Calendar?>?.isExpired(): Boolean {
        val time = this?.second ?: return false
        time.add(
                Calendar.MINUTE,
                dataExpirationTime
        )
        return Calendar.getInstance().after(time)
    }

    fun getHttpResponseCache(url: String): String? {
        cache.httpResponseDict[url]?.also {
            if (!it.isExpired()) {
                return it.first
            } else cache.httpResponseDict.remove(url)
        }
        return null
    }

    fun cacheHttpResponse(url: String, response: String) {
        cache.httpResponseDict[url] = Pair(response, Calendar.getInstance())
    }

    fun existsAppRelease(
            hubUuid: String, auth: Map<String, String?>, appIdList: Map<String, String?>
    ): Boolean {
        val key = hubUuid + auth + appIdList
        val releaseInfoDict = cache.appStatusDict
        return releaseInfoDict.containsKey(key) && !releaseInfoDict[key].isExpired()
    }

    fun getAppRelease(
            hubUuid: String, auth: Map<String, String?>, appIdList: Map<String, String?>
    ): List<ReleaseListItem>? {
        val key = hubUuid + auth + appIdList
        cache.appStatusDict[key]?.also {
            if (!it.isExpired()) {
                return it.first
            } else cache.httpResponseDict.remove(key)
        }
        return null
    }

    fun cacheAppStatus(
            hubUuid: String, auth: Map<String, String?>, appIdList: Map<String, String?>,
            releaseList: List<ReleaseListItem>?
    ) {
        val key = hubUuid + auth + appIdList
        cache.appStatusDict[key] = Pair(releaseList, Calendar.getInstance())
    }

    data class Cache(
            internal val httpResponseDict: MutableMap<String,
                    Pair<String, Calendar>> = mutableMapOf(),
            internal val appStatusDict: MutableMap<String,
                    Pair<List<ReleaseListItem>?, Calendar>> = mutableMapOf()
    )
}
