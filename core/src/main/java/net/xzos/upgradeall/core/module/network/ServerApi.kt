package net.xzos.upgradeall.core.module.network

import net.xzos.upgradeall.core.data.json.DownloadItem
import net.xzos.upgradeall.core.data.json.ReleaseGson

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