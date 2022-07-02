package net.xzos.upgradeall.core.websdk.api.client_proxy.hubs

import net.xzos.upgradeall.core.utils.data_cache.DataCacheManager
import net.xzos.upgradeall.core.websdk.api.web.proxy.OkhttpProxy
import net.xzos.upgradeall.core.websdk.base_model.ApiRequestData
import net.xzos.upgradeall.core.websdk.json.AssetGson
import net.xzos.upgradeall.core.websdk.json.DownloadItem
import net.xzos.upgradeall.core.websdk.json.ReleaseGson

abstract class BaseHub(
    protected val dataCache: DataCacheManager,
    protected val okhttpProxy: OkhttpProxy
) {
    abstract val uuid: String

    abstract fun getRelease(
        data: ApiRequestData,
    ): List<ReleaseGson>?

    open fun getDownload(
        data: ApiRequestData,
        assetIndex: List<Int>,
        assetGson: AssetGson?
    ): List<DownloadItem>? {
        return null
    }
}