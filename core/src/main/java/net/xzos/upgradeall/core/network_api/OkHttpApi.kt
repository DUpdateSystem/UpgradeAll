package net.xzos.upgradeall.core.network_api

import android.R.attr.password
import com.squareup.okhttp.MultipartBuilder
import net.xzos.upgradeall.core.data.json.nongson.ObjectTag
import net.xzos.upgradeall.core.log.Log
import okhttp3.*
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException


object OkHttpApi {

    private const val TAG = "OkHttpApi"
    private val okHttpClient = OkHttpClient().newBuilder().build()
    private val cacheControl = CacheControl.Builder().noCache().build() // 关闭缓存，避免缓存无效（但是属于服务器正常返回的）数据

    fun get(
            objectTag: ObjectTag,
            url: String, headers: Map<String, String> = mapOf()
    ): Response? {
        val request = makeRequest(objectTag, url, headers)
                ?: return null
        return callRequest(objectTag, request.build())
    }

    fun post(
            objectTag: ObjectTag,
            url: String, headers: Map<String, String> = mapOf(),
            bodyType: String, bodyText: String
    ): Response? {
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
        val request = makeRequest(objectTag, url, headers)
                ?: return null
        return callRequest(objectTag, request.post(body).build())
    }

    private fun callRequest(objectTag: ObjectTag, request: Request): Response? {
        return try {
            okHttpClient.newCall(request).execute()
        } catch (e: IOException) {
            Log.e(objectTag, TAG,
                    """getHttpResponse: 网络错误 
                            |ERROR_MESSAGE: $e""".trimIndent()
            )
            null
        }
    }

    private fun makeRequest(
            objectTag: ObjectTag,
            url: String, headers: Map<String, String> = mapOf()
    ): Request.Builder? {
        return try {
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
        }
    }
}
