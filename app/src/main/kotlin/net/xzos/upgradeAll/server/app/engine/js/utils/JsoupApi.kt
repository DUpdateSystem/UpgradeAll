package net.xzos.upgradeAll.server.app.engine.js.utils

import net.xzos.upgradeAll.data.json.nongson.JSCache
import net.xzos.upgradeAll.data.json.nongson.MyCookieManager
import net.xzos.upgradeAll.server.ServerContainer
import org.jsoup.Connection
import org.jsoup.HttpStatusException
import org.jsoup.Jsoup
import org.jsoup.UnsupportedMimeTypeException
import org.jsoup.nodes.Document
import java.io.IOException


internal class JsoupApi(private val logObjectTag: Array<String>) {

    private val jsCache = JSCache(logObjectTag)

    internal var requestHeaders: HashMap<String, String> = hashMapOf()

    fun getDoc(URL: String, userAgent: String? = null,
               method: Connection.Method = Connection.Method.GET): Document? =
            jsCache.getJsoupDomCache(URL)?.also {
                Log.d(logObjectTag, TAG, "getDoc: $URL 已缓存")
            } ?: getRawDoc(URL, userAgent = userAgent, method = method)
                    ?.also {
                        jsCache.cacheJsoupDom(URL, it)
                        Log.d(logObjectTag, TAG, "getDoc: $URL 已刷新")
                    }

    private fun getRawDoc(URL: String, userAgent: String? = null,
                          method: Connection.Method = Connection.Method.GET): Document? {
        return try {
            getRes(URL, userAgent, method).parse()
        } catch (e: IOException) {
            e.printStackTrace()
            Log.e(logObjectTag, TAG, "getRawDoc: Jsoup 对象初始化失败")
            null
        }
    }

    private fun getRes(URL: String, userAgent: String? = null,
                       method: Connection.Method = Connection.Method.GET,
                       headers: Map<String, String> = requestHeaders): Connection.Response {
        val connection = Jsoup
                .connect(URL)
                .followRedirects(true)
                .cookies(MyCookieManager.getCookies(URL))
                .method(method)
                .headers(headers)
                .also {
                    if (userAgent != null) {
                        it.userAgent(userAgent)
                    }
                }
        val res = connection.execute()
        MyCookieManager.setCookies(res.url().toString(), res.cookies())
        requestHeaders = connection.request().headers() as HashMap<String, String>
        return res
    }

    internal fun getRedirectsUrl(URL: String, userAgent: String? = null,
                                 method: Connection.Method = Connection.Method.GET,
                                 headers: Map<String, String> = requestHeaders): String? {

        return try {
            getRes(URL, userAgent, method, headers).url().toString()
        } catch (e: UnsupportedMimeTypeException) {
            Log.d(logObjectTag, TAG, "getRedirectsUrl: 非文本链接指向（下载链接已获取）: $e")
            e.url
        } catch (e: HttpStatusException) {
            Log.e(logObjectTag, TAG, "getRedirectsUrl: 无法获取链接信息: $e")
            null
        } catch (e: Throwable) {
            Log.e(logObjectTag, TAG, "getRedirectsUrl: ERROR_MESSAGE: $e")
            null
        }
    }

    companion object {
        private val Log = ServerContainer.Log
        private const val TAG = "JsoupApi"
    }
}