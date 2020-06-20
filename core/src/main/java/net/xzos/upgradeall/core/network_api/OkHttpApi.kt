package net.xzos.upgradeall.core.network_api

import net.xzos.upgradeall.core.data.json.nongson.ObjectTag
import net.xzos.upgradeall.core.log.Log
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException


object OkHttpApi {

    private const val TAG = "OkHttpApi"
    private val okHttpClient = OkHttpClient().newBuilder().build()
    private val cacheControl = CacheControl.Builder().noCache().build() // 关闭缓存，避免缓存无效（但是属于服务器正常返回的）数据
    var requestHeaders = hashMapOf<String, String>()
        private set

    fun getHttpResponse(
            objectTag: ObjectTag,
            url: String, headers: Map<String, String> = mapOf()
    ): Response? {
        try {
            Request.Builder().cacheControl(cacheControl).url(url).apply {
                for (key in headers.keys)
                    addHeader(key, headers.getValue(key))
            }
        } catch (e: IllegalArgumentException) {
            Log.e(objectTag, TAG,
                    """getHttpResponse: URL: $url 
                        |$e """.trimMargin()
            )
            null
        }?.let { builder ->
            val request = builder.build()
            return try {
                okHttpClient.newCall(request).execute()
            } catch (e: IOException) {
                Log.e(objectTag, TAG,
                        """getHttpResponse: 网络错误 
                            |ERROR_MESSAGE: $e""".trimIndent()
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
        }
        return null
    }
}
