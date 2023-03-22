package net.xzos.upgradeall.core.websdk.api

import net.xzos.upgradeall.core.websdk.base_model.MultiRequestData
import net.xzos.upgradeall.core.websdk.base_model.SingleRequestData
import net.xzos.upgradeall.core.websdk.json.CloudConfigList
import net.xzos.upgradeall.core.websdk.json.DownloadItem
import net.xzos.upgradeall.core.websdk.json.ReleaseGson

interface BaseApi {
    fun getCloudConfig(url: String): CloudConfigList?

    fun checkAppAvailable(data: SingleRequestData): Boolean?

    fun getAppUpdate(data: MultiRequestData): Map<Map<String, String?>, ReleaseGson>?

    fun getAppReleaseList(data: SingleRequestData): List<ReleaseGson>?

    fun getDownloadInfo(
        data: SingleRequestData, assetIndex: Pair<Int, Int>
    ): List<DownloadItem>?
}