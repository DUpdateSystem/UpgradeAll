package net.xzos.upgradeall.core.websdk.api

import net.xzos.upgradeall.core.websdk.base_model.AppData
import net.xzos.upgradeall.core.websdk.base_model.MultiRequestData
import net.xzos.upgradeall.core.websdk.base_model.SingleRequestData

interface BaseApi {
    fun getCloudConfig(url: String): net.xzos.upgradeall.websdk.data.json.CloudConfigList?

    fun checkAppAvailable(data: SingleRequestData): Boolean?

    fun getAppUpdate(data: MultiRequestData): Map<AppData, net.xzos.upgradeall.websdk.data.json.ReleaseGson?>?

    fun getAppReleaseList(data: SingleRequestData): List<net.xzos.upgradeall.websdk.data.json.ReleaseGson>?

    fun getDownloadInfo(
        data: SingleRequestData, assetIndex: Pair<Int, Int>
    ): List<net.xzos.upgradeall.websdk.data.json.DownloadItem>?
}