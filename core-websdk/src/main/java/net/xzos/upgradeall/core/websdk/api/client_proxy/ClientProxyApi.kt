package net.xzos.upgradeall.core.websdk.api.client_proxy

import net.xzos.upgradeall.core.websdk.api.BaseApi
import net.xzos.upgradeall.core.websdk.api.client_proxy.hubs.BaseHub
import net.xzos.upgradeall.core.websdk.api.client_proxy.hubs.Github
import net.xzos.upgradeall.core.websdk.base_model.ApiRequestData
import net.xzos.upgradeall.core.websdk.json.CloudConfigList
import net.xzos.upgradeall.core.websdk.json.DownloadItem
import net.xzos.upgradeall.core.websdk.json.ReleaseGson

class ClientProxyApi : BaseApi {
    private val hubMap: Map<String, BaseHub> = listOf(
        Github()
    ).associateBy({ it.uuid }, { it })

    override suspend fun getCloudConfig(url: String): CloudConfigList? {
        return null
    }

    override fun getAppRelease(data: ApiRequestData): List<ReleaseGson>? {
        val hubUuid = data.hubUuid
        val hub = hubMap[hubUuid]
        return hub?.getRelease(data.appId, data.auth)
    }

    override fun getAppReleaseList(data: ApiRequestData): List<ReleaseGson>? {
        val hubUuid = data.hubUuid
        val hub = hubMap[hubUuid]
        return hub?.getRelease(data.appId, data.auth)
    }

    override suspend fun getDownloadInfo(
        data: ApiRequestData,
        assetIndex: Pair<Int, Int>
    ): List<DownloadItem>? {
        val hubUuid = data.hubUuid
        val hub = hubMap[hubUuid]
        return hub?.getDownload(data.appId, data.auth, assetIndex.toList())
    }
}