package net.xzos.upgradeall.core.websdk.api

import net.xzos.upgradeall.core.websdk.base_model.AppData
import net.xzos.upgradeall.core.websdk.base_model.MultiRequestData
import net.xzos.upgradeall.core.websdk.base_model.SingleRequestData
import net.xzos.upgradeall.websdk.data.json.CloudConfigList
import net.xzos.upgradeall.websdk.data.json.DownloadItem
import net.xzos.upgradeall.websdk.data.json.ReleaseGson

interface BaseApi {
    fun getCloudConfig(url: String): CloudConfigList?

    fun checkAppAvailable(data: SingleRequestData): Boolean?

    fun getAppUpdate(data: MultiRequestData): Map<AppData, ReleaseGson?>?

    fun getAppReleaseList(data: SingleRequestData): List<ReleaseGson>?

    fun getDownloadInfo(
        data: SingleRequestData, assetIndex: Pair<Int, Int>
    ): List<DownloadItem>?
}