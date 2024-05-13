package net.xzos.upgradeall.core.websdk.api

import net.xzos.upgradeall.core.websdk.base_model.AppData
import net.xzos.upgradeall.core.websdk.base_model.MultiRequestData
import net.xzos.upgradeall.core.websdk.base_model.SingleRequestData

class ServerApiProxy internal constructor(
    private val getServerApi: () -> ServerApi?
) : BaseApi {
    private val serverApi get() = getServerApi()

    override fun getCloudConfig(url: String): net.xzos.upgradeall.websdk.data.json.CloudConfigList? {
        return serverApi?.getCloudConfig(url)
    }

    override fun checkAppAvailable(data: SingleRequestData): Boolean? {
        return serverApi?.checkAppAvailable(data)
    }

    override fun getAppUpdate(data: MultiRequestData): Map<AppData, net.xzos.upgradeall.websdk.data.json.ReleaseGson?>? {
        return serverApi?.getAppUpdate(data)
    }

    override fun getAppReleaseList(data: SingleRequestData): List<net.xzos.upgradeall.websdk.data.json.ReleaseGson>? {
        return serverApi?.getAppReleaseList(data)
    }

    override fun getDownloadInfo(
        data: SingleRequestData,
        assetIndex: Pair<Int, Int>
    ): List<net.xzos.upgradeall.websdk.data.json.DownloadItem>? {
        return serverApi?.getDownloadInfo(data, assetIndex)
    }
}