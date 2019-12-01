package net.xzos.upgradeAll.server.app.engine.js.utils

import com.google.gson.Gson
import net.xzos.upgradeAll.application.MyApplication
import net.xzos.upgradeAll.data.json.nongson.MyCookieManager
import net.xzos.upgradeAll.server.ServerContainer
import net.xzos.upgradeAll.utils.AriaDownloader
import net.xzos.upgradeAll.utils.MiscellaneousUtils
import net.xzos.upgradeAll.utils.VersioningUtils
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

    internal var debugMode = false

    private val jsoupApi = JsoupApi(logObjectTag)
    private val okHttpApi = OkHttpApi(logObjectTag)

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

    fun getHttpResponse(URL: String): String? =
            okHttpApi.getHttpResponse(URL)

    fun selNByJsoupXpath(userAgent: String?, URL: String, xpath: String): ArrayList<*> {
        val doc = jsoupApi.getDoc(URL, userAgent = userAgent) ?: return arrayListOf<Any>()
        val jxDocument = JXDocument.create(doc)
        val nodeStringArrayList = ArrayList<String>()
        for (node in jxDocument.selN(xpath)) {
            nodeStringArrayList.add(node.toString())
        }
        Log.d(logObjectTag, TAG, "selNByJsoupXpath: node_list number: " + nodeStringArrayList.size)
        return nodeStringArrayList
    }

    fun mapOfJsonObject(jsonObject: JSONObject): Map<*, *> {
        return Gson().fromJson(jsonObject.toString(), Map::class.java)
    }

    fun matchVersioningString(versionString: String?): String? =
            VersioningUtils.matchVersioningString(versionString)

    fun downloadFile(fileName: String, URL: String, headers: Map<String, String> = mapOf(), isDebug: Boolean = this.debugMode, externalDownloader: Boolean = false): String? {
        val allHeaders = hashMapOf<String, String>().apply {
            this.putAll(headers)
            this.putAll(jsoupApi.requestHeaders) // 装载由 Jsoup 生成的正常 header
            this.putAll(okHttpApi.requestHeaders) // 装载由 Jsoup 生成的正常 header
            MyCookieManager.getCookiesString(URL)?.let {
                this["Cookie"] = it
            }  // 装载 Cookies
        }
        val resUrl = jsoupApi.getRedirectsUrl(URL, headers = allHeaders)
        return if (!externalDownloader)
            AriaDownloader(isDebug).apply {
                waiteGetDownloadTaskNotification(fileName)
            }.start(
                    fileName, resUrl ?: URL,
                    headers = allHeaders).path
        else {
            MiscellaneousUtils.accessByBrowser(
                    resUrl ?: URL,
                    MyApplication.context
            )
            null
        }
    }

    companion object {
        private val Log = ServerContainer.Log
        private const val TAG = "JSUtils"
    }
}