package net.xzos.upgradeall.core.websdk.api

import net.xzos.upgradeall.core.websdk.base_model.ApiRequestData
import net.xzos.upgradeall.core.websdk.json.CloudConfigList
import net.xzos.upgradeall.core.websdk.json.DownloadItem
import net.xzos.upgradeall.core.websdk.json.ReleaseGson

interface BaseApi {
    fun getCloudConfig(url: String): CloudConfigList?

    fun getAppListRelease(dataList: List<ApiRequestData>): Map<ApiRequestData, List<ReleaseGson>>

    fun getAppRelease(data: ApiRequestData): List<ReleaseGson>?

    fun getAppReleaseList(data: ApiRequestData): List<ReleaseGson>?

    fun getDownloadInfo(
        data: ApiRequestData, assetIndex: Pair<Int, Int>
    ): List<DownloadItem>?
}