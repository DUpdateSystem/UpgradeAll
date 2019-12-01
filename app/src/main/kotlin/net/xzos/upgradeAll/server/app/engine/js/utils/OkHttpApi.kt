package net.xzos.upgradeAll.server.app.engine.js.utils

import net.xzos.upgradeAll.data.json.nongson.JSCache
import net.xzos.upgradeAll.data.json.nongson.MyCookieManager
import net.xzos.upgradeAll.server.ServerContainer
import okhttp3.JavaNetCookieJar
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException


class OkHttpApi(private val logObjectTag: Array<String>) {

    private val jsCache = JSCache(logObjectTag)

    internal var requestHeaders = hashMapOf<String, String>()

    fun getHttpResponse(url: String): String? =
            jsCache.getHttpResponseCache(url)
                    ?: getRawHttpResponse(url)
                            ?.also {
                                jsCache.cacheHttpResponse(url, it)
                            }

    private fun getRawHttpResponse(url: String): String? {
        try {
            Request.Builder().url(url)
        } catch (e: IllegalArgumentException) {
            Log.e(logObjectTag, TAG, """getHttpResponse: ${e.printStackTrace()}
                |URL: $url """.trimMargin())
            null
        }?.let { builder ->
            val request = builder.build()
            val response = try {
                okHttpClient.newCall(request).execute()
            } catch (e: IOException) {
                Log.e(logObjectTag, TAG, "getHttpResponse:  网络错误")
                null
            }?.also {
                requestHeaders = hashMapOf()
                for (name in request.headers.names()) {
                    it.headers[name]?.let { value ->
                        requestHeaders[name] = value
                    }
                }
            }
            return response?.body?.string()
        }
        return null
    }

    companion object {
        private val Log = ServerContainer.Log
        private const val TAG = "OkHttpApi"
        private val okHttpClient = OkHttpClient().newBuilder().cookieJar(JavaNetCookieJar(MyCookieManager)).build()
    }
}
