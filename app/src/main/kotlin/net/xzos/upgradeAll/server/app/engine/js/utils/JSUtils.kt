package net.xzos.upgradeAll.server.app.engine.js.utils

import net.xzos.upgradeAll.application.MyApplication
import net.xzos.upgradeAll.data.json.nongson.MyCookieManager
import net.xzos.upgradeAll.server.ServerContainer
import net.xzos.upgradeAll.utils.AriaDownloader
import net.xzos.upgradeAll.utils.MiscellaneousUtils
import net.xzos.upgradeAll.utils.VersioningUtils
import net.xzos.upgradeAll.utils.network.JsoupApi
import net.xzos.upgradeAll.utils.network.OkHttpApi
import org.mozilla.javascript.Context
import org.mozilla.javascript.NativeArray
import org.mozilla.javascript.NativeStringIterator
import org.mozilla.javascript.ScriptableObject
import org.seimicrawler.xpath.JXDocument


/**
 * 爬虫相关库的打包集合
 * For JavaScript
 */
class JSUtils(
        private val logObjectTag: Pair<String, String>,
        private val scope: ScriptableObject
) {

    internal var debugMode = false

    private lateinit var cx: Context

    private val jsoupApi = JsoupApi(logObjectTag)
    private val okHttpApi = OkHttpApi(logObjectTag)

    fun get(cx: Context): JSUtils = this.also {
        this.cx = cx
    }

    fun getHttpResponse(URL: String): String? =
            okHttpApi.getHttpResponse(URL, catchError = false)

    fun selNByJsoupXpath(userAgent: String?, URL: String, xpath: String): MutableList<String> {
        val doc = jsoupApi.getDoc(URL, userAgent = userAgent)
                ?: return mutableListOf()
        val jxDocument = JXDocument.create(doc)
        val nodeStringList = mutableListOf<String>().apply {
            for (node in jxDocument.selN(xpath)) {
                this.add(node.toString())
            }
        }
        Log.d(logObjectTag, TAG, "selNByJsoupXpath: node_list number: " + nodeStringList.size)
        return nodeStringList
    }

    fun matchVersioningString(versionString: String?): String? =
            VersioningUtils.matchVersioningString(versionString)

    fun downloadFile(fileName: String, URL: String, headers: Map<String, String> = mapOf(),
                     isDebug: Boolean = this.debugMode, externalDownloader: Boolean = false) {
        val allHeaders = hashMapOf<String, String>().apply {
            this.putAll(headers)
            this.putAll(jsoupApi.requestHeaders) // 装载由 Jsoup 生成的正常 header
            this.putAll(okHttpApi.requestHeaders) // 装载由 Jsoup 生成的正常 header
            MyCookieManager.getCookiesString(URL)?.let {
                this["Cookie"] = it
            }  // 装载 Cookies
        }
        val resUrl = jsoupApi.getRedirectsUrl(URL, headers = allHeaders)
        if (!externalDownloader)
            AriaDownloader(isDebug).apply {
                waiteGetDownloadTaskNotification(fileName)
            }.start(
                    fileName, resUrl ?: URL,
                    headers = allHeaders)
        // debug 模式下自动删除
        else {
            MiscellaneousUtils.accessByBrowser(
                    resUrl ?: URL,
                    MyApplication.context
            )
        }
    }

    companion object {
        private val Log = ServerContainer.Log
        private const val TAG = "JSUtils"
    }
}