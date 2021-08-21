package net.xzos.upgradeall.core.websdk

import net.xzos.upgradeall.core.utils.log.Log
import net.xzos.upgradeall.core.utils.log.ObjectTag
import net.xzos.upgradeall.core.utils.log.msg
import okhttp3.*
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit


class OkHttpApi internal constructor() {

    companion object {
        private const val TAG = "OkHttpApi"
    }

    private val dispatcher = Dispatcher().apply {
        maxRequests = 128
    }

    private val client = OkHttpClient().newBuilder()
        .dispatcher(dispatcher)
        .callTimeout(15, TimeUnit.SECONDS)
        .connectTimeout(15, TimeUnit.SECONDS)
        .build()
    private val cacheControl = CacheControl.Builder().noCache().build()
    // 关闭缓存，避免缓存无效（但是属于服务器正常返回的）数据

    fun shutdown() {
        client.dispatcher.executorService.shutdown()
        client.connectionPool.evictAll()
        client.cache?.close()
    }

    fun getWithoutError(
        objectTag: ObjectTag,
        url: String, headers: Map<String, String> = mapOf()
    ): Response? {
        return callCore(objectTag, url) { get(url, headers) }
    }

    fun postWithoutError(
        objectTag: ObjectTag, url: String, headers: Map<String, String> = mapOf(),
        bodyType: String, bodyText: String
    ): Response? {
        return callCore(objectTag, url) { post(url, headers, bodyType, bodyText) }
    }

    fun get(url: String, headers: Map<String, String> = mapOf(), callback: Callback) {
        val request = makeRequest(url, headers)
        client.newCall(request.build()).enqueue(callback)
    }

    fun get(url: String, headers: Map<String, String> = mapOf()): Response {
        val request = makeRequest(url, headers)
        return callRequest(request.build())
    }

    fun post(
        url: String, headers: Map<String, String> = mapOf(),
        bodyType: String, bodyText: String
    ): Response {
        val mediaType = "$bodyType; charset=utf-8".toMediaType()
        val body: RequestBody = when (mediaType.subtype) {
            "json" -> bodyText.toRequestBody(mediaType)
            "form-data" -> {
                val dataJson = JSONObject(bodyText)
                FormBody.Builder()
                    .apply {
                        for (key in dataJson.keys()) {
                            addEncoded(key, dataJson[key] as String)
                        }
                    }
                    .build()
            }
            else -> {
                val dataJson = JSONObject(bodyText)
                MultipartBody.Builder()
                    .setType(mediaType)
                    .apply {
                        for (key in dataJson.keys()) {
                            addFormDataPart(key, dataJson[key] as String)
                        }
                    }
                    .build()
            }
        }
        val request = makeRequest(url, headers)
        return callRequest(request.post(body).build())
    }

    private fun callRequest(request: Request): Response = client.newCall(request).execute()

    private fun makeRequest(
        url: String, headers: Map<String, String> = mapOf()
    ): Request.Builder = Request.Builder().cacheControl(cacheControl)
        .url(url).apply {
            for (key in headers.keys)
                addHeader(key, headers.getValue(key))
        }

    private fun callCore(objectTag: ObjectTag, url: String, core: () -> Response): Response? {
        return try {
            core()
        } catch (e: IllegalArgumentException) {
            Log.e(objectTag, TAG, "getHttpResponse: URL: $url ${e.msg()} ")
            null
        } catch (e: IOException) {
            Log.e(objectTag, TAG, "getHttpResponse: 网络错误 ERROR_MESSAGE: ${e.msg()}")
            null
        } catch (e: Throwable) {
            Log.e(objectTag, TAG, "getHttpResponse: 网络错误 ERROR_MESSAGE: ${e.msg()}")
            null
        }
    }
}

val openOkHttpApi = OkHttpApi()
