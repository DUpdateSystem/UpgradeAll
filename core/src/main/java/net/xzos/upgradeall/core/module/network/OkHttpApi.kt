package net.xzos.upgradeall.core.module.network

import net.xzos.upgradeall.core.log.Log
import net.xzos.upgradeall.core.log.ObjectTag
import net.xzos.upgradeall.core.log.msg
import okhttp3.*
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit


object OkHttpApi {

    private const val TAG = "OkHttpApi"
    private val okHttpClient = OkHttpClient().newBuilder()
        .callTimeout(15, TimeUnit.SECONDS)
        .connectTimeout(15, TimeUnit.SECONDS)
        .build()
    private val cacheControl = CacheControl.Builder().noCache().build()
    // 关闭缓存，避免缓存无效（但是属于服务器正常返回的）数据

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

    fun get(url: String, headers: Map<String, String> = mapOf(), retryNum: Int = 0): Response {
        val request = makeRequest(url, headers)
        return callRequestWithRetry(request.build(), retryNum)
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

    private fun callRequestWithRetry(request: Request, retryNum: Int): Response {
        if (retryNum < 0)
            throw SocketTimeoutException("$TAG, callRequestWithRetry: no retry")
        else return try {
            callRequest(request)
        } catch (e: SocketTimeoutException) {
            callRequestWithRetry(request, retryNum - 1)
        }
    }

    private fun callRequest(request: Request): Response = okHttpClient.newCall(request).execute()

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
