package net.xzos.upgradeall.core.websdk.api

import kotlinx.coroutines.runBlocking
import net.xzos.upgradeall.core.utils.data_cache.DataCacheManager
import net.xzos.upgradeall.core.utils.data_cache.cache_object.Encoder
import net.xzos.upgradeall.core.utils.data_cache.cache_object.SaveMode
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
) : BaseApi {

    private val webApi = WebApi()
    private val webApiProxy = WebApiProxy(host, webApi, dataCache)

    private val clientProxyApi = ClientProxyApi(dataCache)

    fun shutdown() {
        webApi.shutdown()
    }

    fun cancelRequest(requestData: ApiRequestData) {
        webApiProxy.cancelRequest(requestData)
    }

    override fun getCloudConfig(url: String): CloudConfigList? {
        val value = dataCache.get(url, SaveMode.MEMORY_AND_DISK, CloudConfigListEncoder) {
            runBlocking {
                clientProxyApi.getCloudConfig(url) ?: webApiProxy.getCloudConfig(url)
            }?.let { if (it.isEmpty()) null else it }
        }
        return value
    }

    override fun checkAppAvailable(data: ApiRequestData): Boolean? {
        return callOrBack(
            data,
            clientProxyApi::checkAppAvailable,
            webApiProxy::checkAppAvailable
        )
    }

    override fun getAppListRelease(dataList: List<ApiRequestData>): Map<ApiRequestData, List<ReleaseGson>> {
        return callOrBack(
            dataList,
            clientProxyApi::getAppListRelease,
            webApiProxy::getAppListRelease
        )
    }

    override fun getAppRelease(data: ApiRequestData): List<ReleaseGson>? {
        val key = data.getKey()
        return dataCache.get(key, SaveMode.MEMORY_AND_DISK, AppReleaseListEncoder) {
            callOrBack(data, clientProxyApi::getAppRelease, webApiProxy::getAppRelease)
        }
    }

    override fun getAppReleaseList(data: ApiRequestData): List<ReleaseGson>? {
        val key = data.getKey()
        return dataCache.get(key, SaveMode.MEMORY_AND_DISK, AppReleaseListEncoder) {
            callOrBack(data, clientProxyApi::getAppReleaseList, webApiProxy::getAppReleaseList)
        }
    }

    override fun getDownloadInfo(
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
            val releaseList = if (assetIndex.first == 0)
                getAppRelease(data)
            else getAppReleaseList(data)

            val asset = releaseList?.getOrNull(assetIndex.first)
                ?.assetGsonList?.getOrNull(assetIndex.second) ?: return emptyList()
            listOf(
                DownloadItem(
                    asset.fileName, asset.downloadUrl ?: return emptyList(),
                    emptyMap(), emptyMap()
                )
            )
        }
    }
}

private fun <A, T> callOrBack(
    data: A,
    function: (A) -> T,
    failback: (A) -> T
): T {
    return try {
        function(data)
    } catch (e: NoFunction) {
        failback(data)
    }
}

fun <E> DataCacheManager.getOrRenewWithCallback(
    key: String, saveMode: SaveMode, encoder: Encoder<E>,
    callback: (E?) -> Unit,
    renewFun: (ApiRequestData, (E?) -> Unit) -> Unit, data: ApiRequestData
) {
    callback(get(key, saveMode, encoder) { null })
    renewFun(data) {
        cache(key, saveMode, it, encoder)
        callback(it)
    }
}

fun ApiRequestData.getKey() = hubUuid + auth + appId + other