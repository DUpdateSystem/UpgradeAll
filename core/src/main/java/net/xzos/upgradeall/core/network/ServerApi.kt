package net.xzos.upgradeall.core.network

import net.xzos.upgradeall.core.data.json.gson.DownloadItem
import net.xzos.upgradeall.core.data.json.gson.ReleaseGson

object ServerApi {
    internal val invalidHubUuidList = hashSetOf<String>()

    fun getCloudConfig(): String? {
        return WebApi.getCloudConfig()
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
            DataCache.getAppRelease(hubUuid, auth, appId)?.also {
                callback(it)
            } ?: WebApi.getAppRelease(hubUuid, auth, appId, {
                it?.let {
                    DataCache.cacheAppStatus(hubUuid, auth, appId, it)
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
            DataCache.getAppRelease(hubUuid, auth, appId)?.also {
                callback(it)
            } ?: WebApi.getAppReleaseList(hubUuid, auth, appId, {
                it?.let {
                    DataCache.cacheAppStatus(hubUuid, auth, appId, it)
                }
                callback(it)
            })
        }
    }

    fun getDownloadInfo(
        hubUuid: String, auth: Map<String, String>,
        appId: Map<String, String>, assetIndex: Pair<Int, Int>
    ): List<DownloadItem> {
        return WebApi.getDownloadInfo(hubUuid, auth, appId, assetIndex)
    }
}

fun <K, V> getNoNullMap(map: Map<K, V?>): Map<K, V> {
    return map.filterNot { it.value != null } as Map<K, V>
}

