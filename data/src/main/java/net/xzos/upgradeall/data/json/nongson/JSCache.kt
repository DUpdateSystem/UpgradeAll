package net.xzos.upgradeall.data.json.nongson

import net.xzos.upgradeall.data.json.gson.JSReturnData
import org.jsoup.nodes.Document
import java.net.*
import java.util.*


object JSCache {

    private val jsCacheData = JSCacheData()
    private val jsNetworkCacheIdMap = hashMapOf<ObjectTag, MutableList<String>>()

    internal var dataExpirationTime = 10

    private fun isExpired(time: Calendar?): Boolean {
        return if (time != null) {
            time.add(Calendar.MINUTE, dataExpirationTime)
            Calendar.getInstance().after(time)
        } else false
    }

    fun clearCache(objectTag: ObjectTag) {
        jsCacheData.jsReturnData.remove(objectTag)  // 删除作为 UI 缓存的 JS 的返回数据
        // 删除网络缓存
        jsNetworkCacheIdMap[objectTag]?.run {
            for (url in this) {
                jsCacheData.httpResponseDict.remove(url)
                jsCacheData.jsoupDomDict.remove(url)
            }
            jsNetworkCacheIdMap.remove(objectTag)
        }
    }

    fun getJsoupDomCache(url: String): Document? {
        val (time, dom) = jsCacheData.jsoupDomDict[url] ?: return null
        return if (isExpired(time)) {
            jsCacheData.jsoupDomDict.remove(url)
            null
        } else dom
    }

    fun cacheJsoupDom(objectTag: ObjectTag, url: String, dom: Document) {
        jsNetworkCacheIdMap[objectTag]?.add(url)
        jsCacheData.jsoupDomDict[url] = Pair(Calendar.getInstance(), dom)
    }

    fun getHttpResponseCache(url: String): String? {
        val (time, response) = jsCacheData.httpResponseDict[url] ?: return null
        return if (isExpired(time)) {
            jsCacheData.httpResponseDict.remove(url)
            null
        } else response
    }

    fun cacheHttpResponse(objectTag: ObjectTag, url: String, response: String) {
        jsNetworkCacheIdMap[objectTag]?.add(url)
        jsCacheData.httpResponseDict[url] = Pair(Calendar.getInstance(), response)
    }

    fun getJsReturnData(objectTag: ObjectTag): JSReturnData? =
            jsCacheData.jsReturnData[objectTag]

    fun cacheJsReturnData(objectTag: ObjectTag, jsReturnData: JSReturnData) {
        jsCacheData.jsReturnData[objectTag] = jsReturnData
    }

    fun clearJsReturnData(objectTag: ObjectTag) {
        jsCacheData.jsReturnData.remove(objectTag)
    }

    private data class JSCacheData(
            internal val httpResponseDict: MutableMap<String, Pair<Calendar, String>> = mutableMapOf(),
            internal val jsoupDomDict: MutableMap<String, Pair<Calendar, Document>> = mutableMapOf(),
            internal val jsReturnData: MutableMap<ObjectTag, JSReturnData> = mutableMapOf()
    )
}

object MyCookieManager : CookieManager() {

    init {
        CookieHandler.setDefault(this)
        this.setCookiePolicy(CookiePolicy.ACCEPT_ALL)
    }

    fun getCookies(URL: String): Map<String, String> {
        val cookies = mutableMapOf<String, String>()
        val httpCookieList = cookieStore.get(URI(URL))
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
