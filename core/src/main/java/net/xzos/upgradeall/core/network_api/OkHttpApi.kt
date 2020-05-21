package net.xzos.upgradeall.core.network_api

import net.xzos.upgradeall.core.data.json.nongson.ObjectTag
import net.xzos.upgradeall.core.data.json.nongson.ObjectTag.Companion.core
import net.xzos.upgradeall.core.data_manager.utils.DataCache
import net.xzos.upgradeall.core.log.Log
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException


object OkHttpApi {

    private const val TAG = "OkHttpApi"
    private val okHttpClient = OkHttpClient().newBuilder().build()
    private val cacheControl = CacheControl.Builder().noCache().build() // 关闭缓存，避免缓存无效（但是属于服务器正常返回的）数据
    var requestHeaders = hashMapOf<String, String>()
        private set

    fun getHttpResponse(
            objectTag: ObjectTag,
            url: String, catchError: Boolean = true
    ): String? =
            DataCache.getHttpResponseCache(url)
                    ?: getRawHttpResponse(objectTag, url, catchError)?.also {
                        DataCache.cacheHttpResponse(url, it)
                    }

    private fun getRawHttpResponse(
            objectTag: ObjectTag,
            url: String, catchError: Boolean
    ): String? {
        try {
            Request.Builder().apply {
                // 对 Core 数据缓存
                if (objectTag.sort != core)
                    cacheControl(cacheControl)
            }.url(url)
        } catch (e: IllegalArgumentException) {
            Log.e(
                    objectTag,
                    TAG, """getHttpResponse: URL: $url 
                    $e """.trimMargin()
            )
            null
        }?.let { builder ->
            val request = builder.build()
            val response = try {
                okHttpClient.newCall(request).execute()
            } catch (e: IOException) {
                if (objectTag.sort != ObjectTag.core)
                    throw  e
                Log.e(
                        objectTag,
                        TAG, """getHttpResponse: 网络错误 
                    ERROR_MESSAGE: $e""".trimIndent()
                )
                null
            } finally {
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
                Log.e(
                        objectTag,
                        TAG, """getHttpResponse: 网络错误（OTHER_ERROR）
                    ERROR_MESSAGE: $e""".trimIndent()
                )
                null
            }
        }
        return null
    }
}
