package net.xzos.upgradeAll.server.app.engine.js.utils

import net.xzos.upgradeAll.data.json.nongson.JSCacheData
import net.xzos.upgradeAll.server.ServerContainer
import okhttp3.JavaNetCookieJar
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class OkHttpApi(private val logObjectTag: Array<String>, private val jsCacheData: JSCacheData = JSCacheData()) {

    fun getHttpResponse(url: String): Pair<String?, OkHttpClient> {
        val client = OkHttpClient().newBuilder().cookieJar(JavaNetCookieJar(jsCacheData.cookieManager)).build()
        try {
            Request.Builder().url(url)
        } catch (e: IllegalArgumentException) {
            Log.e(logObjectTag, TAG, """getHttpResponse: ${e.printStackTrace()}
                |URL: $url """.trimMargin())
            null
        }?.let { builder ->
            val request = builder.build()
            val response = try {
                client.newCall(request).execute()
            } catch (e: IOException) {
                Log.e(logObjectTag, TAG, "getHttpResponse:  网络错误")
                null
            }
            return Pair(response?.body?.string(), client)
        }
        return Pair(null, client)
    }

    companion object {
        private val Log = ServerContainer.Log
        private const val TAG = "OkHttpApi"
    }
}
