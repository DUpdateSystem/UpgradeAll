package net.xzos.upgradeall.jscore.utils

import net.xzos.upgradeall.data.json.nongson.ObjectTag
import net.xzos.upgradeall.log.Log
import net.xzos.upgradeall.network_api.JsoupApi
import net.xzos.upgradeall.network_api.OkHttpApi
import net.xzos.upgradeall.system_api.api.IoApi
import org.mozilla.javascript.Context
import org.seimicrawler.xpath.JXDocument


/**
 * 爬虫相关库的打包集合
 * For JavaScript
 */
internal class JSUtils(
        private val objectTag: ObjectTag,
        internal var debugMode: Boolean = false
) {

    private lateinit var cx: Context


    fun get(cx: Context): JSUtils = this.also {
        this.cx = cx
    }

    fun getHttpResponse(URL: String): String? =
            OkHttpApi.getHttpResponse(objectTag, URL, catchError = false)

    fun selNByJsoupXpath(userAgent: String?, URL: String, xpath: String): MutableList<String> {
        val doc = JsoupApi.getDoc(objectTag, URL, userAgent = userAgent)
                ?: return mutableListOf()
        val jxDocument = JXDocument.create(doc)
        val nodeStringList = mutableListOf<String>().apply {
            for (node in jxDocument.selN(xpath)) {
                this.add(node.toString())
            }
        }
        Log.d(objectTag, TAG, "selNByJsoupXpath: node_list number: " + nodeStringList.size)
        return nodeStringList
    }

    fun matchVersioningString(versionString: String?): String? =
            VersioningUtils.matchVersioningString(versionString)

    fun downloadFile(fileName: String, URL: String, headers: Map<String, String> = mapOf(),
                     isDebug: Boolean = debugMode, externalDownloader: Boolean = false) {
        val allHeaders = headers
                .plus(JsoupApi.requestHeaders) // 装载由 Jsoup 生成的正常 header
                .plus(OkHttpApi.requestHeaders) // 装载由 Jsoup 生成的正常 header,
        IoApi.downloadFile(fileName,
                JsoupApi.getRedirectsUrl(objectTag, URL, headers = allHeaders) ?: URL,
                allHeaders, isDebug, externalDownloader)
    }

    companion object {
        private const val TAG = "JSUtils"
    }
}
