package net.xzos.upgradeall.core.websdk.api.client_proxy.hubs

import net.xzos.upgradeall.core.utils.data_cache.DataCacheManager
import net.xzos.upgradeall.core.websdk.api.web.proxy.OkhttpProxy
import net.xzos.upgradeall.core.websdk.base_model.AppData
import net.xzos.upgradeall.core.websdk.base_model.HubData

abstract class BaseHub(
    protected val dataCache: DataCacheManager, protected val okhttpProxy: OkhttpProxy
) {
    abstract val uuid: String

    abstract fun checkAppAvailable(hub: HubData, app: AppData): Boolean?

    open fun getUpdate(hub: HubData, appList: Collection<AppData>): Map<AppData, net.xzos.upgradeall.websdk.data.json.ReleaseGson?>? =
        null

    abstract fun getReleases(hub: HubData, app: AppData): List<net.xzos.upgradeall.websdk.data.json.ReleaseGson>?

    open fun getDownload(
        hub: HubData, app: AppData,
        assetIndex: List<Int>, assetGson: net.xzos.upgradeall.websdk.data.json.AssetGson?
    ): List<net.xzos.upgradeall.websdk.data.json.DownloadItem>? {
        return null
    }
}