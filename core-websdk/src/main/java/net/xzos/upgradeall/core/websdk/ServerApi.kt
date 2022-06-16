package net.xzos.upgradeall.core.websdk

import net.xzos.upgradeall.core.utils.coroutines.ValueLock
import net.xzos.upgradeall.core.utils.data_cache.DataCacheManager
import net.xzos.upgradeall.core.utils.data_cache.cache_object.Encoder
import net.xzos.upgradeall.core.websdk.base_model.ApiRequestData
import net.xzos.upgradeall.core.websdk.cache.AppReleaseListEncoder
import net.xzos.upgradeall.core.websdk.cache.CloudConfigListEncoder
import net.xzos.upgradeall.core.websdk.json.CloudConfigList
import net.xzos.upgradeall.core.websdk.json.DownloadItem
import net.xzos.upgradeall.core.websdk.json.ReleaseGson
import net.xzos.upgradeall.core.websdk.json.isEmpty
import net.xzos.upgradeall.core.websdk.web.WebApi
import net.xzos.upgradeall.core.websdk.web.WebApiProxy

class ServerApi internal constructor(host: String, private val dataCache: DataCacheManager) {

    private val webApi = WebApi()
    private val webApiProxy = WebApiProxy(host, webApi, dataCache)

    fun shutdown() {
        webApi.shutdown()
    }

    fun cancelRequest(requestData: ApiRequestData) {
        webApiProxy.cancelRequest(requestData)
    }

    suspend fun getCloudConfig(url: String): CloudConfigList? {
        return dataCache.get(url, CloudConfigListEncoder) {
            webApi.getCloudConfig(url)?.let { if (it.isEmpty()) it else null }
        }
    }

    fun getAppRelease(data: ApiRequestData, callback: (List<ReleaseGson>?) -> Unit) {
        val key = data.getKey()
        dataCache.getOrRenewWithCallback(
            key, AppReleaseListEncoder, callback,
            webApiProxy::getAppRelease, data
        )
    }

    fun getAppReleaseList(data: ApiRequestData, callback: (List<ReleaseGson>?) -> Unit) {
        val key = data.getKey()
        dataCache.getOrRenewWithCallback(
            key, AppReleaseListEncoder, callback,
            webApiProxy::getAppReleaseList, data
        )
    }

    suspend fun getDownloadInfo(
        data: ApiRequestData, assetIndex: Pair<Int, Int>
    ): List<DownloadItem> {
        val downloadItemList = try {
            webApiProxy.getDownloadInfo(data, assetIndex)
        } catch (e: Throwable) {
            return emptyList()
        }
        return if (downloadItemList.isNotEmpty())
            downloadItemList
        else {
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

fun ApiRequestData.getKey() = hubUuid + auth + appId