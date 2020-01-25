package net.xzos.upgradeAll.utils.network

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.xzos.upgradeAll.data.json.nongson.JSCache
import net.xzos.upgradeAll.data.json.nongson.MyCookieManager
import net.xzos.upgradeAll.server.ServerContainer
import org.jsoup.Connection
import org.jsoup.HttpStatusException
import org.jsoup.Jsoup
import org.jsoup.UnsupportedMimeTypeException
import org.jsoup.nodes.Document
import java.io.IOException


internal class JsoupApi(private val logObjectTag: Pair<String, String>) {

    private val jsCache = JSCache(logObjectTag)

    internal var requestHeaders: HashMap<String, String> = hashMapOf()

    fun getDoc(url: String, userAgent: String? = null,
               method: Connection.Method = Connection.Method.GET): Document? =
            jsCache.getJsoupDomCache(url)?.also {
                Log.d(logObjectTag, TAG, "getDoc: $url 已缓存")
            } ?: getRawDoc(url, userAgent = userAgent, method = method)
                    ?.also {
                        jsCache.cacheJsoupDom(url, it)
                        Log.d(logObjectTag, TAG, "getDoc: $url 已刷新")
                    }

    private fun getRawDoc(url: String, userAgent: String? = null,
                          method: Connection.Method = Connection.Method.GET): Document? {
        return try {
            getRes(url, userAgent, method).parse()
        } catch (e: IOException) {
            e.printStackTrace()
            Log.e(logObjectTag, TAG, "getRawDoc: Jsoup 对象初始化失败")
            null
        }
    }

    internal fun getRedirectsUrl(url: String, userAgent: String? = null,
                                 method: Connection.Method = Connection.Method.GET,
                                 headers: Map<String, String> = requestHeaders): String? {

        return try {
            getRes(url, userAgent, method, headers).url().toString()
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

    private fun getRes(url: String, userAgent: String? = null,
                       method: Connection.Method = Connection.Method.GET,
                       headers: Map<String, String> = requestHeaders): Connection.Response {
        val mutex =
                mutexMap[url]
                        ?: Mutex().also {
                            mutexMap[url] = it
                        }
        // 获取锁
        return runBlocking {
            // 阻塞获取数据
            mutex.withLock {
                getResNoBlock(url, userAgent, method, headers).also {
                    mutexMap.remove(url)
                }
            }
        }
    }

    private fun getResNoBlock(url: String, userAgent: String? = null,
                              method: Connection.Method = Connection.Method.GET,
                              headers: Map<String, String> = requestHeaders): Connection.Response {
        val connection = Jsoup
                .connect(url)
                .followRedirects(true)
                .cookies(MyCookieManager.getCookies(url))
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

    companion object {
        private val Log = ServerContainer.Log
        private const val TAG = "JsoupApi"
        private val mutexMap: HashMap<String, Mutex> = hashMapOf()
    }
}