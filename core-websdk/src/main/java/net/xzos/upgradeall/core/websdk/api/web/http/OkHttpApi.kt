package net.xzos.upgradeall.core.websdk.api.web.http

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

    fun getCall(data: HttpRequestData): Call {
        val request = makeRequest(data)
        return client.newCall(request.build())
    }

    fun getExecute(data: HttpRequestData): Response {
        return getCall(data).execute()
    }

    private fun makeRequest(
        data: HttpRequestData
    ): Request.Builder = Request.Builder().cacheControl(cacheControl)
        .url(data.url).apply {
            for ((key, value) in data.headers)
                addHeader(key, value)
        }

    fun postRequest(
        data: HttpRequestData,
        bodyType: String, bodyText: String
    ): Request {
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
        val request = makeRequest(data)
        return request.post(body).build()
    }

    companion object {
        private const val TAG = "OkHttpApi"

        fun callHttpFunc(objectTag: ObjectTag, url: String, core: () -> Response): Response? {
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
}

val openOkHttpApi = OkHttpApi()
