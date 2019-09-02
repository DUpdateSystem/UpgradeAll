package net.xzos.upgradeAll.server.app.engine.js.utils

import net.xzos.upgradeAll.json.cache.JSCacheData
import net.xzos.upgradeAll.server.ServerContainer
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.seimicrawler.xpath.JXDocument
import java.util.*

/**
 * 爬虫相关库的打包集合
 * For JavaScript
 */
class JSUtils(private val logObjectTag: Array<String>) {

    private var jsCacheData = JSCacheData()

    fun setJsCacheData(jsCacheData: JSCacheData) {
        this.jsCacheData = jsCacheData
    }

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
        val httpResponseDict = jsCacheData.httpResponseDict
        var responseString: String? = null
        if (httpResponseDict.has(URL)) {
            try {
                responseString = httpResponseDict.getString(URL)
                Log.d(logObjectTag, TAG, "getHttpResponse: 从缓存加载, URL: $URL")
            } catch (e: JSONException) {
                Log.e(logObjectTag, TAG, "getHttpResponse: HTTP 缓存队列无该对象, httpResponseDict : $httpResponseDict")
            }

        } else {
            responseString = OkHttpApi.getHttpResponse(logObjectTag, URL)
            if (responseString != null) {
                try {
                    httpResponseDict.put(URL, responseString)
                    Log.d(logObjectTag, TAG, "getHttpResponse: 缓存, URL: $URL")
                } catch (e: JSONException) {
                    Log.d(logObjectTag, TAG, "getHttpResponse: 缓存失败, URL: $URL")
                }

            }
        }
        return responseString
    }

    fun selNByJsoupXpath(userAgent: String?, URL: String, xpath: String): ArrayList<*> {
        var doc: Document? = Document(URL)
        val jsoupDomDict = jsCacheData.jsoupDomDict
        if (jsoupDomDict.has(URL)) {
            try {
                doc = jsoupDomDict.get(URL) as Document
                Log.d(logObjectTag, TAG, "selNByJsoupXpathJavaList: 从缓存加载, URL: $URL")
            } catch (e: JSONException) {
                Log.e(logObjectTag, TAG, "selNByJsoupXpathJavaList: Jsoup 缓存队列无该对象, jsoupDomDict: $jsoupDomDict")
            }

        } else {
            val connection = Jsoup.connect(URL)
            if (userAgent != null) connection.userAgent(userAgent)
            doc = JsoupApi.getDoc(connection)
            if (doc == null) {
                Log.e(logObjectTag, TAG, "selNByJsoupXpathJavaList: Jsoup 对象初始化失败")
                return ArrayList<Any>()
            }
            try {
                jsoupDomDict.put(URL, doc)
                Log.d(logObjectTag, TAG, "selNByJsoupXpathJavaList: 缓存, URL: $URL")
            } catch (e: JSONException) {
                Log.d(logObjectTag, TAG, "selNByJsoupXpathJavaList: 缓存失败, URL: $URL")
            }

        }
        val jxDocument = JXDocument.create(doc!!)
        val nodeStringArrayList = ArrayList<String>()
        for (node in jxDocument.selN(xpath)) {
            nodeStringArrayList.add(node.toString())
        }
        Log.d(logObjectTag, TAG, "selNByJsoupXpath: node_list number: " + nodeStringArrayList.size)
        return nodeStringArrayList
    }

    companion object {
        private val Log = ServerContainer.AppServer.log
        private const val TAG = "JSUtils"
    }
}

