package net.xzos.upgradeAll.data.json.nongson

import net.xzos.upgradeAll.R
import net.xzos.upgradeAll.application.MyApplication
import net.xzos.upgradeAll.ui.viewmodels.componnent.EditIntPreference
import org.jsoup.nodes.Document
import java.net.*
import java.util.*

internal class JSCache(logObjectTag: Array<String>) {
    private val jsCacheData = jsCacheDataSet[logObjectTag] ?: JSCacheData().also {
        jsCacheDataSet[logObjectTag] = it
    }

    fun getJsoupDomCache(URL: String): Document? {
        val (time, dom) = jsCacheData.jsoupDomDict[URL] ?: return null
        return if (isFreshness(time)) {
            jsCacheData.jsoupDomDict.remove(URL)
            null
        } else dom
    }

    fun cacheJsoupDom(URL: String, dom: Document) {
        jsCacheData.jsoupDomDict[URL] = Pair(Calendar.getInstance(), dom)
    }

    fun getHttpResponseCache(URL: String): String? {
        val (time, response) = jsCacheData.httpResponseDict[URL] ?: return null
        return if (isFreshness(time)) {
            jsCacheData.httpResponseDict.remove(URL)
            null
        } else response
    }

    fun cacheHttpResponse(URL: String, response: String) {
        jsCacheData.httpResponseDict[URL] = Pair(Calendar.getInstance(), response)
    }

    companion object {
        private val jsCacheDataSet = hashMapOf<Array<String>, JSCacheData>()

        internal fun clearCache(logObjectTag: Array<String>) {
            jsCacheDataSet.remove(logObjectTag)
        }

        private fun isFreshness(time: Calendar?): Boolean {
            return if (time == null)
                false
            else {
                val defaultDataExpirationTime = MyApplication.context.resources.getInteger(R.integer.default_data_expiration_time)  // 默认自动刷新时间 10min
                val autoRefreshMinute = EditIntPreference.getInt("sync_time", defaultDataExpirationTime)
                time.add(Calendar.MINUTE, autoRefreshMinute)
                Calendar.getInstance().before(time)
            }
        }
    }
}

private data class JSCacheData(
        internal val httpResponseDict: MutableMap<String, Pair<Calendar, String>> = mutableMapOf(),
        internal val jsoupDomDict: MutableMap<String, Pair<Calendar, Document>> = mutableMapOf()
)

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