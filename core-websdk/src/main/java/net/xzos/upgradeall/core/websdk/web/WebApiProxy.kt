package net.xzos.upgradeall.core.websdk.web

import com.google.gson.Gson
import net.xzos.upgradeall.core.utils.coroutines.coroutinesMutableMapOf
import net.xzos.upgradeall.core.utils.data_cache.DataCacheManager
import net.xzos.upgradeall.core.utils.log.Log
import net.xzos.upgradeall.core.utils.log.ObjectTag
import net.xzos.upgradeall.core.utils.log.msg
import net.xzos.upgradeall.core.websdk.base_model.ApiRequestData
import net.xzos.upgradeall.core.websdk.json.DownloadItem
import net.xzos.upgradeall.core.websdk.json.ReleaseGson
import net.xzos.upgradeall.core.websdk.web.http.DnsApi
import net.xzos.upgradeall.core.websdk.web.http.HttpRequestData

internal class WebApiProxy(
    private val _host: String,
    private val webApi: WebApi,
    private val dataCache: DataCacheManager,
) {
    private val host: String
        get() = dataCache.get(_host, null) {
            try {
                DnsApi.resolve(_host)
            } catch (e: Throwable) {
                Log.e(objectTag, TAG, "resolveHost: Error: ${e.msg()}")
                null
            }
        } ?: _host

    private val requestDataMap = coroutinesMutableMapOf<ApiRequestData, HttpRequestData>()

    fun getAppRelease(data: ApiRequestData, callback: (List<ReleaseGson>?) -> Unit) {
        val appIdPath = getMapPath(data.appId)
        val hubUuid = data.hubUuid
        val authHeader = getAuthHeaderDict(data.auth)
        val url = "http://$host/v1/app/${hubUuid}/${appIdPath}/release"
        val httpRequestData = HttpRequestData(url, authHeader, markId = hubUuid).apply {
            requestDataMap[data] = this
        }
        webApi.getAppRelease(httpRequestData) {
            requestDataMap.remove(data)
            callback(it)
        }
    }

    fun getAppReleaseList(data: ApiRequestData, callback: (List<ReleaseGson>?) -> Unit) {
        val appIdPath = getMapPath(data.appId)
        val hubUuid = data.hubUuid
        val url = "http://$host/v1/app/${hubUuid}/${appIdPath}/releases"
        val authHeader = getAuthHeaderDict(data.auth)
        val httpRequestData = HttpRequestData(url, authHeader, markId = hubUuid).apply {
            requestDataMap[data] = this
        }
        webApi.getAppReleaseList(httpRequestData) {
            requestDataMap.remove(data)
            try {
                callback(it)
            } catch (e: Throwable) {
                Log.e(objectTag, TAG, "getAppReleaseList: Error: ${e.msg()}")
                callback(null)
            }
        }
    }

    suspend fun getDownloadInfo(
        data: ApiRequestData,
        assetIndex: Pair<Int, Int>
    ): List<DownloadItem> {
        val appIdPath = getMapPath(data.appId)
        val hubUuid = data.hubUuid
        val authHeader = getAuthHeaderDict(data.auth)
        val assetIndexPath = getIntListPath(assetIndex.toList())
        val url = "http://$host/v1/app/${hubUuid}/${appIdPath}/extra_download/$assetIndexPath"
        val httpRequestData = HttpRequestData(url, authHeader, markId = hubUuid).apply {
            requestDataMap[data] = this
        }
        return webApi.getDownloadInfo(httpRequestData).apply {
            requestDataMap.remove(data)
        }
    }

    fun cancelRequest(requestData: ApiRequestData) {
        requestDataMap[requestData]?.apply {
            webApi.cancelCall(this)
        }
    }

    companion object {
        private const val TAG = "WebApiProxy"
        private val objectTag = ObjectTag(ObjectTag.core, TAG)

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
}