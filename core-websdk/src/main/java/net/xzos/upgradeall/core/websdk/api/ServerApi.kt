package net.xzos.upgradeall.core.websdk.api

import kotlinx.coroutines.runBlocking
import net.xzos.upgradeall.core.utils.coroutines.ValueLock
import net.xzos.upgradeall.core.utils.data_cache.DataCacheManager
import net.xzos.upgradeall.core.utils.data_cache.cache_object.Encoder
import net.xzos.upgradeall.core.websdk.api.client_proxy.ClientProxyApi
import net.xzos.upgradeall.core.websdk.api.client_proxy.NoFunction
import net.xzos.upgradeall.core.websdk.api.web.WebApi
import net.xzos.upgradeall.core.websdk.api.web.WebApiProxy
import net.xzos.upgradeall.core.websdk.base_model.ApiRequestData
import net.xzos.upgradeall.core.websdk.cache.AppReleaseListEncoder
import net.xzos.upgradeall.core.websdk.cache.CloudConfigListEncoder
import net.xzos.upgradeall.core.websdk.json.CloudConfigList
import net.xzos.upgradeall.core.websdk.json.DownloadItem
import net.xzos.upgradeall.core.websdk.json.ReleaseGson
import net.xzos.upgradeall.core.websdk.json.isEmpty

class ServerApi internal constructor(
    host: String, private val dataCache: DataCacheManager
) {

    private val webApi = WebApi()
    private val webApiProxy = WebApiProxy(host, webApi, dataCache)

    private val clientProxyApi = ClientProxyApi(dataCache)

    fun shutdown() {
        webApi.shutdown()
    }

    fun cancelRequest(requestData: ApiRequestData) {
        webApiProxy.cancelRequest(requestData)
    }

    fun getCloudConfig(url: String): CloudConfigList? {
        val value = dataCache.get(url, CloudConfigListEncoder) {
            runBlocking {
                clientProxyApi.getCloudConfig(url) ?: webApiProxy.getCloudConfig(url)
            }?.let { if (it.isEmpty()) null else it }
        }
        return value
    }

    fun getAppRelease(data: ApiRequestData, callback: (List<ReleaseGson>?) -> Unit) {
        val key = data.getKey()
        val value = dataCache.get(key, AppReleaseListEncoder) {
            callOrBack(data, clientProxyApi::getAppRelease, webApiProxy::getAppRelease)
        }
        callback(value)
    }

    fun getAppReleaseList(data: ApiRequestData, callback: (List<ReleaseGson>?) -> Unit) {
        val key = data.getKey()
        val value = dataCache.get(key, AppReleaseListEncoder) {
            callOrBack(data, clientProxyApi::getAppReleaseList, webApiProxy::getAppReleaseList)
        }
        callback(value)
    }

    suspend fun getDownloadInfo(
        data: ApiRequestData, assetIndex: Pair<Int, Int>
    ): List<DownloadItem> {
        val downloadItemList = try {
            callOrBack(
                data,
                { d -> clientProxyApi.getDownloadInfo(d, assetIndex) },
                { d -> webApiProxy.getDownloadInfo(d, assetIndex) })
        } catch (e: Throwable) {
            return emptyList()
        }
        return downloadItemList.ifEmpty {
            val releaseListLock = ValueLock<List<ReleaseGson>>()
            if (assetIndex.first == 0)
                getAppRelease(data) { releaseListLock.setValue(it) }
            else getAppReleaseList(data) { releaseListLock.setValue(it) }
            val releaseList = releaseListLock.getValue()
            val asset = releaseList?.getOrNull(assetIndex.first)
                ?.assetList?.getOrNull(assetIndex.second) ?: return emptyList()
            listOf(
                DownloadItem(
                    asset.fileName, asset.downloadUrl ?: return emptyList(),
                    emptyMap(), emptyMap()
                )
            )
        }
    }
}

private fun <T> callOrBack(
    data: ApiRequestData,
    function: (ApiRequestData) -> T,
    callback: (ApiRequestData) -> T
): T {
    return try {
        function(data)
    } catch (e: NoFunction) {
        callback(data)
    }
}

fun <E> DataCacheManager.getOrRenewWithCallback(
    key: String, encoder: Encoder<E>,
    callback: (E?) -> Unit,
    renewFun: (ApiRequestData, (E?) -> Unit) -> Unit, data: ApiRequestData
) {
    callback(get(key, encoder) { null })
    renewFun(data) {
        cache(key, it, encoder)
        callback(it)
    }
}

fun ApiRequestData.getKey() = hubUuid + auth + appId + other