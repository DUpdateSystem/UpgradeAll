package net.xzos.upgradeAll.server.app.engine.js.utils

import android.widget.Toast
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.xzos.upgradeAll.application.MyApplication
import net.xzos.upgradeAll.json.nongson.JSCacheData
import net.xzos.upgradeAll.server.ServerContainer
import net.xzos.upgradeAll.utils.AriaDownloader
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.seimicrawler.xpath.JXDocument
import java.util.*


/**
 * 爬虫相关库的打包集合
 * For JavaScript
 */
class JSUtils(private val logObjectTag: Array<String>) {

    internal var isDebug = false

    private var jsoupApi = JsoupApi(logObjectTag, jsCacheData)

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
            response = OkHttpApi(logObjectTag, jsCacheData = jsCacheData).getHttpResponse(URL).first
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
        var doc = jsoupDomDict[URL]?.second
        if (doc == null || !JSCacheData.isFreshness(time)) {
            doc = jsoupApi.getDoc(URL, userAgent = userAgent)
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

    fun downloadFile(fileName: String, URL: String, headers: Map<String, String> = mapOf(), isDebug: Boolean = this.isDebug): String? {
        val allHeaders: MutableMap<String, String> = mutableMapOf()
        allHeaders.putAll(headers)
        allHeaders.putAll(jsoupApi.requestHeaders) // 装载由 Jsoup 生成的正常 header
        // 装载 Cookies
        val cookieString = jsCacheData.cookieManager.getCookiesString(URL)
        allHeaders["Cookie"] = cookieString
        val ariaDownloader = AriaDownloader(isDebug).apply {
            waiteGetDownloadTaskNotification(fileName)
        }
        val resUrl = jsoupApi.getRedirectsUrl(URL)
        return if (resUrl != null) {
            ariaDownloader.start(fileName, resUrl, headers = allHeaders).path
        } else {
            GlobalScope.launch(Dispatchers.Main) { Toast.makeText(MyApplication.context, "无法获取下载链接", Toast.LENGTH_SHORT).show() }
            ariaDownloader.cancel()
            null
        }
    }

    fun mapOfJsonObject(jsonObject: JSONObject): Map<*, *> {
        return Gson().fromJson(jsonObject.toString(), Map::class.java)
    }

    companion object {
        private val Log = ServerContainer.Log
        private const val TAG = "JSUtils"

        private val jsCacheData = JSCacheData()
    }
}