package net.xzos.upgradeAll.server.app.engine.js.utils

import net.xzos.upgradeAll.data.json.nongson.JSCacheData
import net.xzos.upgradeAll.server.ServerContainer
import okhttp3.JavaNetCookieJar
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

class OkHttpApi(private val logObjectTag: Array<String>, private val jsCacheData: JSCacheData = JSCacheData()) {

    fun getHttpResponse(url: String): Pair<String?, OkHttpClient> {
        val client = OkHttpClient().newBuilder().cookieJar(JavaNetCookieJar(jsCacheData.cookieManager)).build()
        var response: Response? = null
        val builder = Request.Builder()
        builder.url(url)
        val request = builder.build()
        try {
            response = client.newCall(request).execute()
        } catch (e: IOException) {
            Log.e(logObjectTag, TAG, "getHttpResponse:  网络错误")
        }
        return Pair(response?.body?.string(), client)
    }

    companion object {
        private val Log = ServerContainer.Log
        private const val TAG = "OkHttpApi"
    }
}
