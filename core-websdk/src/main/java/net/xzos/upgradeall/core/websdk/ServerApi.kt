package net.xzos.upgradeall.core.websdk

import android.util.Log
import net.xzos.upgradeall.core.utils.coroutines.ValueLock
import net.xzos.upgradeall.core.utils.data_cache.DataCache
import net.xzos.upgradeall.core.websdk.base_model.ApiRequestData
import net.xzos.upgradeall.core.websdk.json.CloudConfigList
import net.xzos.upgradeall.core.websdk.json.DownloadItem
import net.xzos.upgradeall.core.websdk.json.ReleaseGson
import net.xzos.upgradeall.core.websdk.json.isEmpty
import net.xzos.upgradeall.core.websdk.web.WebApi
import net.xzos.upgradeall.core.websdk.web.WebApiProxy

class ServerApi internal constructor(host: String, dataCacheTimeSec: Int) {
    companion object;

    private val webApi = WebApi()
    private val dataCache = DataCache(dataCacheTimeSec)
    private val webApiProxy = WebApiProxy(host, webApi, dataCache)

    fun shutdown() {
        webApi.shutdown()
    }

    fun cancelRequest(requestData: ApiRequestData) {
        webApiProxy.cancelRequest(requestData)
    }

    suspend fun getCloudConfig(url: String): CloudConfigList? {
        return dataCache.get(url) ?: webApi.getCloudConfig(url)?.also {
            if (!it.isEmpty()) dataCache.cache(url, it)
        }
    }

    fun getAppRelease(data: ApiRequestData, callback: (List<ReleaseGson>?) -> Unit) {
        dataCache.getAppRelease(data)?.also {
            callback(it)
        } ?: webApiProxy.getAppRelease(data) {
            it?.let {
                dataCache.cacheAppStatus(data, it)
            }
            callback(it)
        }
    }

    fun getAppReleaseList(data: ApiRequestData, callback: (List<ReleaseGson>?) -> Unit) {
        dataCache.getAppRelease(data)?.also {
            callback(it)
        } ?: webApiProxy.getAppReleaseList(data) {
            it?.let {
                dataCache.cacheAppStatus(data, it)
            }
            callback(it)
        }
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

fun DataCache.getAppRelease(
    data: ApiRequestData,
): List<ReleaseGson>? {
    val key = data.getKey()
    return get(key)
}

fun DataCache.cacheAppStatus(
    data: ApiRequestData,
    releaseList: List<ReleaseGson>?
) {
    if (this.getAppRelease(data) != releaseList) {
        val key = data.getKey()
        cache(key, releaseList)
    }
}

fun ApiRequestData.getKey() = hubUuid + auth + appId