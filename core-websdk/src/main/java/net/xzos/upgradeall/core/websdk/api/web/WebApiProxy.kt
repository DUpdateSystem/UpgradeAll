package net.xzos.upgradeall.core.websdk.api.web

import com.google.gson.Gson
import net.xzos.upgradeall.core.utils.coroutines.ValueMutex
import net.xzos.upgradeall.core.utils.coroutines.coroutinesMutableMapOf
import net.xzos.upgradeall.core.utils.data_cache.DataCacheManager
import net.xzos.upgradeall.core.utils.data_cache.cache_object.SaveMode
import net.xzos.upgradeall.core.utils.log.Log
import net.xzos.upgradeall.core.utils.log.ObjectTag
import net.xzos.upgradeall.core.utils.log.msg
import net.xzos.upgradeall.core.websdk.api.BaseApi
import net.xzos.upgradeall.core.websdk.api.web.http.DnsApi
import net.xzos.upgradeall.core.websdk.api.web.http.HttpRequestData
import net.xzos.upgradeall.core.websdk.base_model.AppData
import net.xzos.upgradeall.core.websdk.base_model.HubData
import net.xzos.upgradeall.core.websdk.base_model.MultiRequestData
import net.xzos.upgradeall.core.websdk.base_model.SingleRequestData
import net.xzos.upgradeall.websdk.data.json.CloudConfigList
import net.xzos.upgradeall.websdk.data.json.DownloadItem
import net.xzos.upgradeall.websdk.data.json.ReleaseGson

internal class WebApiProxy(
    private val _host: String,
    private val webApi: WebApi,
    private val dataCache: DataCacheManager,
) : BaseApi {
    private val host: String
        get() = dataCache.get(mutex, SaveMode.MEMORY_ONLY, _host, null) {
            try {
                DnsApi.resolve(_host)
            } catch (e: Throwable) {
                Log.e(objectTag, TAG, "resolveHost: Error: ${e.msg()}")
                null
            }
        } ?: _host
    private val requestDataMap = coroutinesMutableMapOf<String, HttpRequestData>()

    override fun getCloudConfig(url: String): CloudConfigList? {
        return webApi.getCloudConfig(url)
    }

    override fun checkAppAvailable(data: SingleRequestData): Boolean? = null

    override fun getAppUpdate(data: MultiRequestData): Map<AppData, ReleaseGson?>? {
        return data.appList.mapNotNull {
            getAppRelease(data.hub, it)?.firstOrNull()?.let { r -> it to r }
        }.toMap()
    }

    private fun getAppRelease(hub: HubData, app: AppData): List<ReleaseGson>? {
        val appIdPath = getMapPath(app.appId + app.other)
        val hubUuid = hub.hubUuid
        val authHeader = getAuthHeaderDict(hub.auth)
        val url = "http://$host/v1/app/${hubUuid}/${appIdPath}/release"
        val mapKey = getRequestMapKey(hub, app)
        val httpRequestData = HttpRequestData(url, authHeader, markId = hubUuid).apply {
            requestDataMap[mapKey] = this
        }
        return webApi.getAppRelease(httpRequestData).apply {
            requestDataMap.remove(mapKey)
        }
    }

    override fun getAppReleaseList(data: SingleRequestData): List<ReleaseGson>? {
        val appIdPath = getMapPath(data.app.appId + data.app.other)
        val hubUuid = data.hub.hubUuid
        val url = "http://$host/v1/app/${hubUuid}/${appIdPath}/releases"
        val authHeader = getAuthHeaderDict(data.hub.auth)
        val mapKey = getRequestMapKey(data.hub, data.app)
        val httpRequestData = HttpRequestData(url, authHeader, markId = hubUuid).apply {
            requestDataMap[mapKey] = this
        }
        return webApi.getAppReleaseList(httpRequestData).apply {
            requestDataMap.remove(mapKey)
        }
    }

    override fun getDownloadInfo(
        data: SingleRequestData,
        assetIndex: Pair<Int, Int>
    ): List<DownloadItem>? {
        val appIdPath = getMapPath(data.app.appId + data.app.other)
        val hubUuid = data.hub.hubUuid
        val authHeader = getAuthHeaderDict(data.hub.auth)
        val assetIndexPath = getIntListPath(assetIndex.toList())
        val url = "http://$host/v1/app/${hubUuid}/${appIdPath}/extra_download/$assetIndexPath"
        val mapKey = getRequestMapKey(data.hub, data.app)
        val httpRequestData = HttpRequestData(url, authHeader, markId = hubUuid).apply {
            requestDataMap[mapKey] = this
        }
        return webApi.getDownloadInfo(httpRequestData).apply {
            requestDataMap.remove(mapKey)
        }
    }

    private fun getRequestMapKey(hub: HubData, app: AppData): String {
        return "${hub.getStringId()}-${app.getStringId()}"
    }

    fun cancelRequest(hub: HubData, app: AppData) {
        requestDataMap[getRequestMapKey(hub, app)]?.apply {
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

        private val mutex = ValueMutex()
    }
}