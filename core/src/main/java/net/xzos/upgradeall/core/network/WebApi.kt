package net.xzos.upgradeall.core.network

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import net.xzos.upgradeall.core.data.config.AppConfig
import net.xzos.upgradeall.core.data.json.gson.DownloadItem
import net.xzos.upgradeall.core.data.json.gson.ReleaseGson
import net.xzos.upgradeall.core.data.json.nongson.ObjectTag
import net.xzos.upgradeall.core.data.json.nongson.ObjectTag.Companion.core
import net.xzos.upgradeall.core.log.Log
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException
import java.net.SocketTimeoutException

internal object WebApi {
    private val host = AppConfig.update_server_url
    private const val TAG = "WebApi"
    private val objectTag = ObjectTag(core, TAG)

    private val invalidHubUuidList = ServerApi.invalidHubUuidList

    fun getCloudConfig(): String? {
        val url = "http://$host/v1/rules/download/master"
        return try {
            callApiCore {
                OkHttpApi.get(url)
            }?.body?.string()
        }catch (e:Throwable){
            Log.w(objectTag, TAG, "getCloudConfig: {$e.stackTraceToString()}")
            null
        }
    }

    fun getAppRelease(
        hubUuid: String, auth: Map<String, String>, appId: Map<String, String>,
        callback: (List<ReleaseGson>?) -> Unit, retryNum: Int = 2
    ) {
        if (retryNum < 0 || hubUuid in invalidHubUuidList) callback(null)
        val appIdPath = getMapPath(appId)
        val authHeader = getAuthHeaderDict(auth)
        val url = "http://$host/v1/app/$hubUuid/${appIdPath}/release"
        doOkhttpCall(hubUuid, url, authHeader, {
            if (it == null)
                callback(null)
            else {
                when (it.code) {
                    200 -> {
                        val responseStr = it.body?.string()
                        try {
                            val release = Gson().fromJson(responseStr, ReleaseGson::class.java)
                            callback(listOf(release))
                        } catch (e: Throwable) {
                            Log.w(objectTag, TAG, "getAppRelease: {$e.stackTraceToString()}")
                            callback(emptyList())
                        }
                    }
                    410 -> callback(emptyList())
                    else -> {
                        Log.e(objectTag, TAG, "getAppRelease: $it")
                        callback(null)
                    }
                }
            }
        })
    }

    fun getAppReleaseList(
        hubUuid: String, auth: Map<String, String>, appId: Map<String, String>,
        callback: (List<ReleaseGson>?) -> Unit, retryNum: Int = 2,
    ) {
        if (retryNum < 0 || hubUuid in invalidHubUuidList) callback(null)
        val appIdPath = getMapPath(appId)
        val url = "http://$host/v1/app/$hubUuid/${appIdPath}/releases"
        val authHeader = getAuthHeaderDict(auth)
        doOkhttpCall(hubUuid, url, authHeader, {
            if (it == null)
                callback(null)
            else {
                when (it.code) {
                    200 -> {
                        val responseStr = it.body?.string()
                        val listType = object : TypeToken<ArrayList<ReleaseGson>>() {}.type
                        val releaseList = try {
                            Gson().fromJson<List<ReleaseGson>>(responseStr, listType)
                        } catch (e: Throwable) {
                            Log.w(objectTag, TAG, "getAppReleaseList: {$e.stackTraceToString()}")
                            emptyList()
                        }
                        callback(releaseList)
                    }
                    410 -> callback(emptyList())
                    else -> {
                        Log.e(objectTag, TAG, "getAppReleaseList: $it")
                        callback(null)
                    }
                }
            }
        })
    }

    fun getDownloadInfo(
        hubUuid: String, auth: Map<String, String>,
        appId: Map<String, String>, assetIndex: Pair<Int, Int>
    ): List<DownloadItem> {
        val appIdPath = getMapPath(appId)
        val authHeader = getAuthHeaderDict(auth)
        val assetIndexPath = getIntListPath(assetIndex.toList())
        val url = "http://$host/v1/app/$hubUuid/${appIdPath}/extra_download/$assetIndexPath"
        val response = callApiCore { OkHttpApi.get(url, authHeader) }
        val responseStr = response?.body?.string() ?: return emptyList()
        if (response.code != 200) return emptyList()
        val listType = object : TypeToken<ArrayList<DownloadItem>>() {}.type
        return try {
            Gson().fromJson(responseStr, listType)
        } catch (e: Throwable) {
            Log.w(objectTag, TAG, "getDownloadInfo: {$e.stackTraceToString()}")
            emptyList()
        }
    }

    private fun callApiCore(retryNum: Int = 3, core: () -> Response): Response? {
        return if (retryNum < 0)
            null
        else try {
            val response = core()
            if (response.code == 408) {
                Log.w(objectTag, TAG, "callApiCore: Server timeout: $response")
                callApiCore(retryNum - 1, core)
            } else response
        } catch (e: SocketTimeoutException) {
            callApiCore(retryNum - 1, core)
        } catch (e: Throwable) {
            Log.e(objectTag, TAG, "callApiCore: ${e.stackTraceToString()}")
            null
        }
    }

    fun doOkhttpCall(
        hubUuid: String, url: String, headers: Map<String, String>,
        callback: (Response?) -> Unit, retryNum: Int = 2
    ) {
        if (retryNum < 0 || hubUuid in invalidHubUuidList) callback(null)
        OkHttpApi.get(url, headers, object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(
                    objectTag, TAG,
                    "doOkhttpCall: url: ${call.request().url}, e: ${e.stackTraceToString()}"
                )
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                when (response.code) {
                    408 -> {
                        Log.w(objectTag, TAG, "doOkhttpCall: Server timeout: $response")
                        doOkhttpCall(hubUuid, url, headers, callback, retryNum - 1)
                        return
                    }
                    400 -> if (response.body?.string() == "no hub: $hubUuid")
                        invalidHubUuidList.add(hubUuid)
                }
                callback(response)
            }
        })
    }

    private fun getMapPath(appId: Map<String, String?>): String {
        var appIdPath = ""
        for ((k, v) in appId) {
            appIdPath += "/$k/$v"
        }
        return appIdPath.replaceFirst("/", "")
    }

    private fun getAuthHeaderDict(auth: Map<String, String>): Map<String, String> {
        return mapOf("Authorization" to Gson().toJson(auth))
    }

    private fun getIntListPath(list: Collection<Int>) = list.joinToString("/")
}