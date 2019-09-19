package net.xzos.upgradeAll.server.app.engine.js.utils

import net.xzos.upgradeAll.json.nongson.JSCacheData
import net.xzos.upgradeAll.server.ServerContainer
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.jsoup.Jsoup
import org.seimicrawler.xpath.JXDocument
import java.util.*

/**
 * 爬虫相关库的打包集合
 * For JavaScript
 */
class JSUtils(private val logObjectTag: Array<String>) {

    fun getJSONObject(): JSONObject {
        return JSONObject()
    }

    fun getJSONArray(): JSONArray {
        return JSONArray()
    }

    @Throws(JSONException::class)
    fun getJSONObject(jsonString: String? = null): JSONObject {
        return if (jsonString != null) JSONObject(jsonString)
        else JSONObject()
    }

    @Throws(JSONException::class)
    fun getJSONArray(jsonString: String? = null): JSONArray {
        return if (jsonString != null) JSONArray(jsonString)
        else JSONArray()
    }

    fun getHttpResponse(URL: String): String? {
        val httpResponseMap = jsCacheData.httpResponseDict
        val time = httpResponseMap[URL]?.first
        var response = httpResponseMap[URL]?.second
        if (response == null || !JSCacheData.isFreshness(time)) {
            response = OkHttpApi.getHttpResponse(logObjectTag, URL).first
            if (response != null) {
                httpResponseMap[URL] = Pair(Calendar.getInstance(), response)
                Log.d(logObjectTag, TAG, "OkHttp: $URL 已刷新")
            }
        } else {
            Log.d(logObjectTag, TAG, "OkHttp: $URL 已缓存")
        }
        return response
    }

    fun selNByJsoupXpath(userAgent: String?, URL: String, xpath: String): ArrayList<*> {
        val jsoupDomDict = jsCacheData.jsoupDomDict
        val time = jsoupDomDict[URL]?.first
        val connection = Jsoup.connect(URL)
        if (userAgent != null) connection.userAgent(userAgent)
        var doc = jsoupDomDict[URL]?.second
        if (doc == null || !JSCacheData.isFreshness(time)) {
            doc = JsoupApi.getDoc(connection)
            if (doc != null) {
                jsoupDomDict[URL] = Pair(Calendar.getInstance(), doc)
                Log.d(logObjectTag, TAG, "Jsoup: $URL 已刷新")
            } else {
                Log.e(logObjectTag, TAG, "selNByJsoupXpathJavaList: Jsoup 对象初始化失败")
                return ArrayList<Any>()
            }
        } else
            Log.d(logObjectTag, TAG, "Jsoup: $URL 已缓存")
        val jxDocument = JXDocument.create(doc)
        val nodeStringArrayList = ArrayList<String>()
        for (node in jxDocument.selN(xpath)) {
            nodeStringArrayList.add(node.toString())
        }
        Log.d(logObjectTag, TAG, "selNByJsoupXpath: node_list number: " + nodeStringArrayList.size)
        return nodeStringArrayList
    }

    companion object {
        private val Log = ServerContainer.Log
        private const val TAG = "JSUtils"

        private var jsCacheData = JSCacheData()
    }
}

