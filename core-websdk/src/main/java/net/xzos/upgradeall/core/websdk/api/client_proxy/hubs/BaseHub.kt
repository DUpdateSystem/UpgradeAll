package net.xzos.upgradeall.core.websdk.api.client_proxy.hubs

import net.xzos.upgradeall.core.utils.data_cache.DataCacheManager
import net.xzos.upgradeall.core.websdk.api.web.proxy.OkhttpProxy
import net.xzos.upgradeall.core.websdk.base_model.MultiRequestData
import net.xzos.upgradeall.core.websdk.base_model.SingleRequestData
import net.xzos.upgradeall.core.websdk.json.AssetGson
import net.xzos.upgradeall.core.websdk.json.DownloadItem
import net.xzos.upgradeall.core.websdk.json.ReleaseGson

abstract class BaseHub(
    protected val dataCache: DataCacheManager, protected val okhttpProxy: OkhttpProxy
) {
    abstract val uuid: String

    abstract fun checkAppAvailable(data: SingleRequestData): Boolean?

    open fun getUpdate(data: MultiRequestData): Map<Map<String, String?>, ReleaseGson> {
        return data.appIdList.mapNotNull {
            getReleases(
                SingleRequestData(data.hubUuid, data.auth, it, data.other)
            )?.firstOrNull()?.let { r -> it to r }
        }.toMap()
    }

    abstract fun getReleases(data: SingleRequestData): List<ReleaseGson>?

    open fun getDownload(
        data: SingleRequestData, assetIndex: List<Int>, assetGson: AssetGson?
    ): List<DownloadItem>? {
        return null
    }
}