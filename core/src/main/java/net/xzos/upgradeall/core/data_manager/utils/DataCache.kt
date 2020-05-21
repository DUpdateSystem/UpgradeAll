package net.xzos.upgradeall.core.data_manager.utils

import net.xzos.upgradeall.core.data.config.AppValue
import net.xzos.upgradeall.core.route.AppIdItem
import net.xzos.upgradeall.core.route.AppStatus
import java.util.*


object DataCache {

    private val cache = Cache()

    private var dataExpirationTime = AppValue.data_expiration_time

    private fun List<AppIdItem>.cacheKey(hubUuid: String): String? {
        if (this.isEmpty()) return null
        var key = hubUuid
        for (i in this) {
            key += "+${i.value}"
        }
        return key
    }

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

    fun existsAppStatus(
            hubUuid: String,
            appId: List<AppIdItem>
    ): Boolean {
        val key = appId.cacheKey(hubUuid)
        val releaseInfoDict = cache.appStatusDict
        return releaseInfoDict.containsKey(key) && !releaseInfoDict[key].isExpired()
    }

    fun getAppStatus(
            hubUuid: String,
            appId: List<AppIdItem>
    ): AppStatus? {
        val key = appId.cacheKey(hubUuid) ?: return null
        cache.appStatusDict[key]?.also {
            if (!it.isExpired()) {
                return it.first
            } else cache.httpResponseDict.remove(key)
        }
        return null
    }

    fun cacheReleaseInfo(
            hubUuid: String,
            appId: List<AppIdItem>,
            appStatus: AppStatus
    ) {
        val key = appId.cacheKey(hubUuid) ?: return
        cache.appStatusDict[key] = Pair(appStatus, Calendar.getInstance())
    }

    data class Cache(
            internal val httpResponseDict: MutableMap<String,
                    Pair<String, Calendar>> = mutableMapOf(),
            internal val appStatusDict: MutableMap<String,
                    Pair<AppStatus, Calendar>> = mutableMapOf()
    )
}
