package net.xzos.upgradeall.core.data_manager.utils

import net.xzos.upgradeall.core.data.config.AppConfig
import net.xzos.upgradeall.core.data.json.gson.WebApiGetGson
import net.xzos.upgradeall.core.data.json.gson.WebApiReturnGson
import net.xzos.upgradeall.core.data.json.gson.key
import java.net.*
import java.util.*


object DataCache {

    private val cache = Cache()

    private var dataExpirationTime = AppConfig.data_expiration_time

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

    fun existsCache(
        hubUuid: String,
        appInfoList: List<WebApiGetGson.AppInfoListBean>
    ): Boolean {
        val key = appInfoList.key(hubUuid)
        val releaseInfoDict = cache.releaseInfoDict
        return releaseInfoDict.containsKey(key) && !releaseInfoDict[key].isExpired()
    }

    fun getReleaseInfo(
        hubUuid: String,
        appInfoList: List<WebApiGetGson.AppInfoListBean>
    ): List<WebApiReturnGson.ReleaseInfoBean>? {
        val key = appInfoList.key(hubUuid) ?: return null
        cache.releaseInfoDict[key]?.also {
            if (!it.isExpired()) {
                return it.first
            } else cache.httpResponseDict.remove(key)
        }
        return null
    }

    fun cacheReleaseInfo(
            hubUuid: String,
            appInfoList: List<WebApiGetGson.AppInfoListBean>,
            releaseInfo: List<WebApiReturnGson.ReleaseInfoBean>?
    ) {
        val key = appInfoList.key(hubUuid) ?: return
        cache.releaseInfoDict[key] = Pair(releaseInfo, Calendar.getInstance())
    }

    data class Cache(
        internal val httpResponseDict: MutableMap<String, Pair<String, Calendar>> = mutableMapOf(),
        internal val releaseInfoDict: MutableMap<String, Pair<
                List<WebApiReturnGson.ReleaseInfoBean>?, Calendar
                >> = mutableMapOf()
    )
}

object MyCookieManager : CookieManager() {

    init {
        CookieHandler.setDefault(this)
        this.setCookiePolicy(CookiePolicy.ACCEPT_ALL)
    }

    fun getCookies(URL: String): Map<String, String> {
        val cookies = mutableMapOf<String, String>()
        val httpCookieList = cookieStore.get(URI(URI(URL).host))
        for (httpCookie in httpCookieList) {
            cookies[httpCookie.name] = httpCookie.value
        }
        return cookies
    }

    fun setCookies(URL: String, cookies: Map<String, String>) {
        val hostUrl = URI(URL).host  // 存储 host 网址
        for (key in cookies.keys) {
            cookieStore.add(URI(hostUrl), HttpCookie(key, cookies[key]))
        }
    }

    fun getCookiesString(URL: String): String? {
        val cookies = getCookies(URL)
        return if (cookies.isNotEmpty()) {
            var cookieString = ""
            for (key in cookies.keys) {
                cookieString += "$key=${cookies[key]}; "
            }
            cookieString.substringBeforeLast(";")
        } else null
    }
}
