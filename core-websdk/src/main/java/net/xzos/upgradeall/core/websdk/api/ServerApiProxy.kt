package net.xzos.upgradeall.core.websdk.api

import net.xzos.upgradeall.core.websdk.base_model.ApiRequestData
import net.xzos.upgradeall.core.websdk.json.CloudConfigList
import net.xzos.upgradeall.core.websdk.json.DownloadItem
import net.xzos.upgradeall.core.websdk.json.ReleaseGson

class ServerApiProxy internal constructor(
    private val getServerApi: () -> ServerApi?
) : BaseApi {
    private val serverApi get() = getServerApi()

    override fun getCloudConfig(url: String): CloudConfigList? {
        return serverApi?.getCloudConfig(url)
    }

    override fun checkAppAvailable(data: ApiRequestData): Boolean? {
        return serverApi?.checkAppAvailable(data)
    }

    override fun getAppListRelease(dataList: List<ApiRequestData>): Map<ApiRequestData, List<ReleaseGson>> {
        return serverApi?.getAppListRelease(dataList) ?: emptyMap()
    }

    override fun getAppRelease(data: ApiRequestData): List<ReleaseGson>? {
        return serverApi?.getAppReleaseList(data)
    }

    override fun getAppReleaseList(data: ApiRequestData): List<ReleaseGson>? {
        return serverApi?.getAppReleaseList(data)
    }

    override fun getDownloadInfo(
        data: ApiRequestData, assetIndex: Pair<Int, Int>
    ): List<DownloadItem>? {
        return serverApi?.getDownloadInfo(data, assetIndex)
    }
}