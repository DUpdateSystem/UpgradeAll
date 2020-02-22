package net.xzos.upgradeall.network_api

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.xzos.upgradeall.data.json.nongson.JSCache
import net.xzos.upgradeall.data.json.nongson.MyCookieManager
import net.xzos.upgradeall.data.json.nongson.ObjectTag
import net.xzos.upgradeall.log.Log
import org.jsoup.Connection
import org.jsoup.HttpStatusException
import org.jsoup.Jsoup
import org.jsoup.UnsupportedMimeTypeException
import org.jsoup.nodes.Document
import java.io.IOException


object JsoupApi {

    private const val TAG = "JsoupApi"
    private val mutexMap: HashMap<String, Mutex> = hashMapOf()
    var requestHeaders: HashMap<String, String> = hashMapOf()
        private set

    fun getDoc(objectTag: ObjectTag,
               url: String, userAgent: String? = null,
               method: Connection.Method = Connection.Method.GET
    ): Document? =
            JSCache.getJsoupDomCache(url).also {
                Log.d(objectTag, TAG, "getDoc: $url 已缓存")
            } ?: getRawDoc(objectTag, url, userAgent = userAgent, method = method)
                    ?.also {
                        JSCache.cacheJsoupDom(objectTag, url, it)
                        Log.d(objectTag, TAG, "getDoc: $url 已刷新")
                    }

    private fun getRawDoc(objectTag: ObjectTag,
                          url: String, userAgent: String? = null,
                          method: Connection.Method = Connection.Method.GET
    ): Document? {
        return try {
            getRes(url, userAgent, method).parse()
        } catch (e: IOException) {
            e.printStackTrace()
            Log.e(objectTag, TAG, "getRawDoc: Jsoup 对象初始化失败")
            null
        }
    }

    fun getRedirectsUrl(objectTag: ObjectTag,
                                 url: String, userAgent: String? = null,
                                 method: Connection.Method = Connection.Method.GET,
                                 headers: Map<String, String> = requestHeaders)
            : String? {

        return try {
            getRes(url, userAgent, method, headers).url().toString()
        } catch (e: UnsupportedMimeTypeException) {
            Log.d(objectTag, TAG, "getRedirectsUrl: 非文本链接指向（下载链接已获取）: $e")
            e.url
        } catch (e: HttpStatusException) {
            Log.e(objectTag, TAG, "getRedirectsUrl: 无法获取链接信息: $e")
            null
        } catch (e: Throwable) {
            Log.e(objectTag, TAG, "getRedirectsUrl: ERROR_MESSAGE: $e")
            null
        }
    }

    private fun getRes(url: String, userAgent: String? = null,
                       method: Connection.Method = Connection.Method.GET,
                       headers: Map<String, String> = requestHeaders
    ): Connection.Response {
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
                              headers: Map<String, String> = requestHeaders
    ): Connection.Response {
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
}
