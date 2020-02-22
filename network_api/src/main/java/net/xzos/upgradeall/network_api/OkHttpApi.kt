package net.xzos.upgradeall.network_api

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.xzos.upgradeall.data.json.nongson.JSCache
import net.xzos.upgradeall.data.json.nongson.MyCookieManager
import net.xzos.upgradeall.data.json.nongson.ObjectTag
import net.xzos.upgradeall.log.Log
import okhttp3.JavaNetCookieJar
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException


object OkHttpApi {

    private const val TAG = "OkHttpApi"
    private val okHttpClient = OkHttpClient().newBuilder().cookieJar(JavaNetCookieJar(MyCookieManager)).build()
    private val mutexMap: HashMap<String, Mutex> = hashMapOf()
    var requestHeaders = hashMapOf<String, String>()
        private set

    fun getHttpResponse(objectTag: ObjectTag,
                        url: String, catchError: Boolean = true
    ): String? {
        val mutex =
                mutexMap[url]
                        ?: Mutex().also {
                            mutexMap[url] = it
                        }
        // 获取锁
        return runBlocking {
            // 阻塞获取数据
            mutex.withLock {
                getHttpResponseNoBlock(objectTag, url, catchError)
                        .also {
                            mutexMap.remove(url)
                        }
            }
        }
    }

    private fun getHttpResponseNoBlock(objectTag: ObjectTag,
                                       url: String, catchError: Boolean = true
    ): String? =
            JSCache.getHttpResponseCache(url)
                    ?: getRawHttpResponse(objectTag, url, catchError)
                            ?.also {
                                JSCache.cacheHttpResponse(objectTag, url, it)
                            }

    private fun getRawHttpResponse(objectTag: ObjectTag,
                                   url: String, catchError: Boolean
    ): String? {
        try {
            Request.Builder().url(url)
        } catch (e: IllegalArgumentException) {
            Log.e(objectTag, TAG, """getHttpResponse: URL: $url
                | $e """.trimMargin())
            null
        }?.let { builder ->
            val request = builder.build()
            val response = try {
                okHttpClient.newCall(request).execute()
            } catch (e: IOException) {
                Log.e(objectTag, TAG, "getHttpResponse: 网络错误")
                null
            }?.also {
                requestHeaders = hashMapOf()
                for (name in request.headers.names()) {
                    it.headers[name]?.let { value ->
                        requestHeaders[name] = value
                    }
                }
            }
            return try {
                response?.body?.string()
            } catch (e: Throwable) {
                if (!catchError) throw e
                Log.e(objectTag, TAG, "getHttpResponse: 网络错误")
                null
            }
        }
        return null
    }
}
