package net.xzos.upgradeall.core.websdk

import net.xzos.upgradeall.core.utils.DataCache
import net.xzos.upgradeall.core.utils.coroutines.ValueLock
import net.xzos.upgradeall.core.utils.coroutines.coroutinesMutableListOf
import net.xzos.upgradeall.core.websdk.json.CloudConfigList
import net.xzos.upgradeall.core.websdk.json.DownloadItem
import net.xzos.upgradeall.core.websdk.json.ReleaseGson

class ServerApi(host: String, dataCacheTimeSec: Int) {
    private val invalidHubUuidList = coroutinesMutableListOf<String>(true)
    private var webApi = WebApi(host, invalidHubUuidList)
    private val dataCache = DataCache(dataCacheTimeSec)

    fun shutdown() {
        webApi.shutdown()
    }

    fun getCloudConfig(): CloudConfigList? {
        return webApi.getCloudConfig()
    }

    fun getAppRelease(
        hubUuid: String,
        auth: Map<String, String>,
        appId: Map<String, String>,
        callback: (List<ReleaseGson>?) -> Unit
    ) {
        if (hubUuid in invalidHubUuidList) {
            callback(null)
        } else {
            dataCache.getAppRelease(hubUuid, auth, appId)?.also {
                callback(it)
            } ?: webApi.getAppRelease(hubUuid, auth, appId, {
                it?.let {
                    dataCache.cacheAppStatus(hubUuid, auth, appId, it)
                }
                callback(it)
            })
        }
    }

    fun getAppReleaseList(
        hubUuid: String, auth: Map<String, String>,
        appId: Map<String, String>, callback: (List<ReleaseGson>?) -> Unit
    ) {
        if (hubUuid in invalidHubUuidList) {
            callback(null)
        } else {
            dataCache.getAppRelease(hubUuid, auth, appId)?.also {
                callback(it)
            } ?: webApi.getAppReleaseList(hubUuid, auth, appId, {
                it?.let {
                    dataCache.cacheAppStatus(hubUuid, auth, appId, it)
                }
                callback(it)
            })
        }
    }

    suspend fun getDownloadInfo(
        hubUuid: String, auth: Map<String, String>,
        appId: Map<String, String>, assetIndex: Pair<Int, Int>
    ): List<DownloadItem> {
        val downloadItemList = webApi.getDownloadInfo(hubUuid, auth, appId, assetIndex)
        return if (downloadItemList.isNotEmpty())
            downloadItemList
        else {
            val releaseListLock = ValueLock<List<ReleaseGson>>()
            if (assetIndex.first == 0)
                webApi.getAppRelease(hubUuid, auth, appId, { releaseListLock.setValue(it) })
            else webApi.getAppReleaseList(hubUuid, auth, appId, { releaseListLock.setValue(it) })
            val releaseList = releaseListLock.getValue()
            val asset = releaseList?.getOrNull(assetIndex.first)
                ?.assetList?.getOrNull(assetIndex.second) ?: return emptyList()
            listOf(DownloadItem(asset.fileName, asset.downloadUrl, emptyMap(), emptyMap()))
        }
    }
}

fun DataCache.getAppRelease(
    hubUuid: String, auth: Map<String, String?>, appId: Map<String, String?>
): List<ReleaseGson>? {
    val key = hubUuid + auth + appId
    return get(key)
}

fun DataCache.cacheAppStatus(
    hubUuid: String, auth: Map<String, String?>, appId: Map<String, String?>,
    releaseList: List<ReleaseGson>?
) {
    val key = hubUuid + auth + appId
    if (this.getAppRelease(hubUuid, auth, appId) != releaseList) {
        cache(key, releaseList)
    }
}
