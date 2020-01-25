package net.xzos.upgradeAll.utils.network

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.xzos.upgradeAll.data.json.nongson.JSCache
import net.xzos.upgradeAll.data.json.nongson.MyCookieManager
import net.xzos.upgradeAll.server.ServerContainer
import okhttp3.JavaNetCookieJar
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException


class OkHttpApi(private val logObjectTag: Pair<String, String>) {

    private val jsCache = JSCache(logObjectTag)

    internal var requestHeaders = hashMapOf<String, String>()

    fun getHttpResponse(url: String, catchError: Boolean = true): String? {
        val mutex =
                mutexMap[url]
                        ?: Mutex().also {
                            mutexMap[url] = it
                        }
        // 获取锁
        return runBlocking {
            // 阻塞获取数据
            mutex.withLock {
                getHttpResponseNoBlock(url, catchError)
                        .also {
                            mutexMap.remove(url)
                        }
            }
        }
    }

    private fun getHttpResponseNoBlock(url: String, catchError: Boolean = true): String? =
            jsCache.getHttpResponseCache(url)
                    ?: getRawHttpResponse(url, catchError)
                            ?.also {
                                jsCache.cacheHttpResponse(url, it)
                            }

    private fun getRawHttpResponse(url: String, catchError: Boolean): String? {
        try {
            Request.Builder().url(url)
        } catch (e: IllegalArgumentException) {
            Log.e(logObjectTag, TAG, """getHttpResponse: URL: $url
                | $e """.trimMargin())
            null
        }?.let { builder ->
            val request = builder.build()
            val response = try {
                okHttpClient.newCall(request).execute()
            } catch (e: IOException) {
                Log.e(logObjectTag, TAG, "getHttpResponse: 网络错误")
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
                Log.e(logObjectTag, TAG, "getHttpResponse: 网络错误")
                null
            }
        }
        return null
    }

    companion object {
        private val Log = ServerContainer.Log
        private const val TAG = "OkHttpApi"
        private val okHttpClient = OkHttpClient().newBuilder().cookieJar(JavaNetCookieJar(MyCookieManager)).build()
        private val mutexMap: HashMap<String, Mutex> = hashMapOf()
    }
}