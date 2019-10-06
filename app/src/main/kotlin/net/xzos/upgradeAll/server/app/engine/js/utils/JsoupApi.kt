package net.xzos.upgradeAll.server.app.engine.js.utils

import net.xzos.upgradeAll.json.nongson.JSCacheData
import net.xzos.upgradeAll.server.ServerContainer
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.IOException
import java.util.*


internal class JsoupApi(private val logObjectTag: Array<String>, private val jsCacheData: JSCacheData) {

    private val cookieManager = jsCacheData.cookieManager

    internal var requestHeaders: Map<String, String> = mapOf()

    fun getDoc(URL: String, userAgent: String? = null, method: Connection.Method = Connection.Method.GET): Document? {
        val jsoupDomDict = jsCacheData.jsoupDomDict
        val time = jsoupDomDict[URL]?.first
        var doc = jsoupDomDict[URL]?.second
        if (doc == null || !JSCacheData.isFreshness(time)) {
            val connection = Jsoup
                    .connect(URL)
                    .cookies(cookieManager.getCookies(URL))
                    .method(method)
            if (userAgent != null) connection.userAgent(userAgent)
            val res = connection.execute()
            doc = try {
                res.parse()
            } catch (e: IOException) {
                e.printStackTrace()
                null
            }
            if (doc != null) {
                jsoupDomDict[URL] = Pair(Calendar.getInstance(), doc)
                Log.d(logObjectTag, TAG, "Jsoup: $URL 已刷新")
                cookieManager.setCookies(URL, res.cookies())
                requestHeaders = connection.request().headers()
            } else {
                Log.e(logObjectTag, TAG, "selNByJsoupXpathJavaList: Jsoup 对象初始化失败")
                return null
            }
        } else
            Log.d(logObjectTag, TAG, "Jsoup: $URL 已缓存")
        return doc
    }

    companion object {
        private val Log = ServerContainer.Log
        private const val TAG = "JsoupApi"
    }
}
