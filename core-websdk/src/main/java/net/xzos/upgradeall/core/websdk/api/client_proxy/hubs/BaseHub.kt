package net.xzos.upgradeall.core.websdk.api.client_proxy.hubs

import net.xzos.upgradeall.core.utils.data_cache.DataCacheManager
import net.xzos.upgradeall.core.websdk.api.web.proxy.OkhttpProxy
import net.xzos.upgradeall.core.websdk.base_model.AppData
import net.xzos.upgradeall.core.websdk.base_model.HubData
import net.xzos.upgradeall.core.websdk.json.AssetGson
import net.xzos.upgradeall.core.websdk.json.DownloadItem
import net.xzos.upgradeall.core.websdk.json.ReleaseGson

abstract class BaseHub(
    protected val dataCache: DataCacheManager, protected val okhttpProxy: OkhttpProxy
) {
    abstract val uuid: String

    abstract fun checkAppAvailable(hub: HubData, app: AppData): Boolean?

    open fun getUpdate(hub: HubData, appList: List<AppData>): Map<AppData, ReleaseGson> {
        return appList.mapNotNull {
            getReleases(hub, it)?.firstOrNull()?.let { r -> it to r }
        }.toMap()
    }

    abstract fun getReleases(hub: HubData, app: AppData): List<ReleaseGson>?

    open fun getDownload(
        hub: HubData, app: AppData,
        assetIndex: List<Int>, assetGson: AssetGson?
    ): List<DownloadItem>? {
        return null
    }
}