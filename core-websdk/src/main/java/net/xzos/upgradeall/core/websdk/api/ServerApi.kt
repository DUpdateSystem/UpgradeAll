package net.xzos.upgradeall.core.websdk.api

import kotlinx.coroutines.runBlocking
import net.xzos.upgradeall.core.utils.coroutines.ValueMutexMap
import net.xzos.upgradeall.core.utils.data_cache.DataCacheManager
import net.xzos.upgradeall.core.utils.data_cache.cache_object.SaveMode
import net.xzos.upgradeall.core.websdk.api.client_proxy.ClientProxyApi
import net.xzos.upgradeall.core.websdk.api.client_proxy.NoFunction
import net.xzos.upgradeall.core.websdk.api.web.WebApi
import net.xzos.upgradeall.core.websdk.api.web.WebApiProxy
import net.xzos.upgradeall.core.websdk.base_model.AppData
import net.xzos.upgradeall.core.websdk.base_model.HubData
import net.xzos.upgradeall.core.websdk.base_model.MultiRequestData
import net.xzos.upgradeall.core.websdk.base_model.SingleRequestData
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

    fun cancelRequest(hub: HubData, app: AppData) {
        webApiProxy.cancelRequest(hub, app)
    }

    override fun getCloudConfig(url: String): CloudConfigList? {
        val value = lockMap.runWith(url) {
            dataCache.get(it, SaveMode.MEMORY_AND_DISK, url, CloudConfigListEncoder) {
                runBlocking {
                    clientProxyApi.getCloudConfig(url) ?: webApiProxy.getCloudConfig(url)
                }?.let { if (it.isEmpty()) null else it }
            }
        }
        return value
    }

    override fun checkAppAvailable(data: SingleRequestData): Boolean? {
        return callOrBack(
            data,
            clientProxyApi::checkAppAvailable,
            webApiProxy::checkAppAvailable
        )
    }

    override fun getAppUpdate(data: MultiRequestData): Map<AppData, ReleaseGson?>? {
        return callOrBack(
            data,
            clientProxyApi::getAppUpdate,
            webApiProxy::getAppUpdate
        )
    }

    override fun getAppReleaseList(data: SingleRequestData): List<ReleaseGson>? {
        val key = data.getKey()
        return lockMap.runWith(key) {
            dataCache.get(it, SaveMode.DISK_ONLY, key, AppReleaseListEncoder) {
                callOrBack(data, clientProxyApi::getAppReleaseList, webApiProxy::getAppReleaseList)
            }
        }
    }

    override fun getDownloadInfo(
        data: SingleRequestData, assetIndex: Pair<Int, Int>
    ): List<DownloadItem> {
        val downloadItemList = try {
            callOrBack(
                data,
                { d -> clientProxyApi.getDownloadInfo(d, assetIndex) },
                { d -> webApiProxy.getDownloadInfo(d, assetIndex) })
        } catch (e: Throwable) {
            return emptyList()
        } ?: emptyList()
        return downloadItemList.ifEmpty {
            val releaseList = if (assetIndex.first == 0)
                getAppReleaseList(data)
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

    companion object {
        private val lockMap = ValueMutexMap()
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

fun SingleRequestData.getKey() = "${hub.getStringId()}-${app.getStringId()}"