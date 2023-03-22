package net.xzos.upgradeall.core.websdk.api.web

import com.google.gson.Gson
import net.xzos.upgradeall.core.utils.coroutines.coroutinesMutableMapOf
import net.xzos.upgradeall.core.utils.data_cache.DataCacheManager
import net.xzos.upgradeall.core.utils.data_cache.cache_object.SaveMode
import net.xzos.upgradeall.core.utils.log.Log
import net.xzos.upgradeall.core.utils.log.ObjectTag
import net.xzos.upgradeall.core.utils.log.msg
import net.xzos.upgradeall.core.websdk.api.BaseApi
import net.xzos.upgradeall.core.websdk.api.web.http.DnsApi
import net.xzos.upgradeall.core.websdk.api.web.http.HttpRequestData
import net.xzos.upgradeall.core.websdk.base_model.ApiRequestData
import net.xzos.upgradeall.core.websdk.base_model.MultiRequestData
import net.xzos.upgradeall.core.websdk.base_model.SingleRequestData
import net.xzos.upgradeall.core.websdk.json.CloudConfigList
import net.xzos.upgradeall.core.websdk.json.DownloadItem
import net.xzos.upgradeall.core.websdk.json.ReleaseGson

internal class WebApiProxy(
    private val _host: String,
    private val webApi: WebApi,
    private val dataCache: DataCacheManager,
) : BaseApi {
    private val host: String
        get() = dataCache.get(_host, SaveMode.MEMORY_ONLY, null) {
            try {
                DnsApi.resolve(_host)
            } catch (e: Throwable) {
                Log.e(objectTag, TAG, "resolveHost: Error: ${e.msg()}")
                null
            }
        } ?: _host
    private val requestDataMap = coroutinesMutableMapOf<ApiRequestData, HttpRequestData>()

    override fun getCloudConfig(url: String): CloudConfigList? {
        return webApi.getCloudConfig(url)
    }

    override fun checkAppAvailable(data: SingleRequestData): Boolean? = null

    override fun getAppUpdate(data: MultiRequestData): Map<Map<String, String?>, ReleaseGson>? {
        return data.appIdList.mapNotNull {
            getAppRelease(
                SingleRequestData(data.hubUuid, data.auth, it, data.other)
            )?.firstOrNull()?.let { r -> it to r }
        }.toMap()
    }

    private fun getAppRelease(data: SingleRequestData): List<ReleaseGson>? {
        val appIdPath = getMapPath(data.appId)
        val hubUuid = data.hubUuid
        val authHeader = getAuthHeaderDict(data.auth)
        val url = "http://$host/v1/app/${hubUuid}/${appIdPath}/release"
        val httpRequestData = HttpRequestData(url, authHeader, markId = hubUuid).apply {
            requestDataMap[data] = this
        }
        return webApi.getAppRelease(httpRequestData).apply {
            requestDataMap.remove(data)
        }
    }

    override fun getAppReleaseList(data: SingleRequestData): List<ReleaseGson>? {
        val appIdPath = getMapPath(data.appId)
        val hubUuid = data.hubUuid
        val url = "http://$host/v1/app/${hubUuid}/${appIdPath}/releases"
        val authHeader = getAuthHeaderDict(data.auth)
        val httpRequestData = HttpRequestData(url, authHeader, markId = hubUuid).apply {
            requestDataMap[data] = this
        }
        return webApi.getAppReleaseList(httpRequestData).apply {
            requestDataMap.remove(data)
        }
    }

    override fun getDownloadInfo(
        data: SingleRequestData,
        assetIndex: Pair<Int, Int>
    ): List<DownloadItem>? {
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

        private fun getAuthHeaderDict(auth: Map<String, String?>): Map<String, String> {
            return mapOf("Authorization" to Gson().toJson(auth))
        }

        private fun getIntListPath(list: Collection<Int>) = list.joinToString("/")
    }
}