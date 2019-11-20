package net.xzos.upgradeAll.data.json.nongson

import net.xzos.upgradeAll.R
import net.xzos.upgradeAll.application.MyApplication
import net.xzos.upgradeAll.ui.viewmodels.componnent.EditIntPreference
import org.jsoup.nodes.Document
import java.net.*
import java.util.*

data class JSCacheData(
        val httpResponseDict: MutableMap<String, Pair<Calendar, String>> = mutableMapOf(),
        val jsoupDomDict: MutableMap<String, Pair<Calendar, Document>> = mutableMapOf(),
        val cookieManager: MyCookieManager = MyCookieManager()
) {
    init {
        CookieHandler.setDefault(cookieManager)
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL)
    }

    companion object {
        fun isFreshness(time: Calendar?): Boolean {
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

class MyCookieManager : CookieManager() {
    fun getCookies(URL: String): Map<String, String> {
        val cookies = mutableMapOf<String, String>()
        val httpCookieList = cookieStore.get(URI(URL))
        for (httpCookie in httpCookieList) {
            cookies[httpCookie.name] = httpCookie.value
        }
        return cookies
    }

    fun setCookies(URL: String, cookies: Map<String, String>) {
        for (key in cookies.keys) {
            cookieStore.add(URI(URL), HttpCookie(key, cookies[key]))
        }
    }

    fun getCookiesString(URL: String): String {
        val cookies = getCookies(URL)
        var cookieString = ""
        for (key in cookies.keys) {
            cookieString += "$key=${cookies[key]}; "
        }
        return cookieString.substringBeforeLast(";")
    }
}