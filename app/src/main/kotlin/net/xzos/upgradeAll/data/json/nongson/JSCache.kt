package net.xzos.upgradeAll.data.json.nongson

import net.xzos.upgradeAll.R
import net.xzos.upgradeAll.application.MyApplication
import net.xzos.upgradeAll.data.json.gson.JSReturnData
import net.xzos.upgradeAll.ui.viewmodels.componnent.EditIntPreference
import net.xzos.upgradeAll.utils.MiscellaneousUtils
import org.jsoup.nodes.Document
import java.net.*
import java.util.*


internal class JSCache(private val objectTag: ObjectTag) {

    fun clearCache() {
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

    fun cacheJsoupDom(url: String, dom: Document) {
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

    fun cacheHttpResponse(url: String, response: String) {
        jsNetworkCacheIdMap[objectTag]?.add(url)
        jsCacheData.httpResponseDict[url] = Pair(Calendar.getInstance(), response)
    }

    fun getJsReturnData(): JSReturnData? {
        val jsReturnData = jsCacheData.jsReturnData[objectTag] ?: return null
        return if (MiscellaneousUtils.isBackground()) {
            jsCacheData.jsReturnData.remove(objectTag)
            null
        } else jsReturnData
    }

    fun cacheJsReturnData(jsReturnData: JSReturnData) {
        jsCacheData.jsReturnData[objectTag] = jsReturnData
    }

    companion object {
        private val jsCacheData = JSCacheData()
        private val jsNetworkCacheIdMap = hashMapOf<ObjectTag, MutableList<String>>()

        private fun isExpired(time: Calendar?): Boolean {
            return if (time != null) {
                val defaultDataExpirationTime = MyApplication.context.resources.getInteger(R.integer.default_data_expiration_time)  // 默认自动刷新时间 10min
                val autoRefreshMinute = EditIntPreference.getInt("sync_time", defaultDataExpirationTime)
                time.add(Calendar.MINUTE, autoRefreshMinute)
                Calendar.getInstance().after(time)
            } else false
        }
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
