package net.xzos.upgradeall.core.websdk.base_model

internal interface BaseServerApi<E> {
    suspend fun getCloudConfig(host: String): net.xzos.upgradeall.websdk.data.json.CloudConfigList?

    fun getAppRelease(data: E, callback: (List<net.xzos.upgradeall.websdk.data.json.ReleaseGson>?) -> Unit)

    fun getAppReleaseList(data: E, callback: (List<net.xzos.upgradeall.websdk.data.json.ReleaseGson>?) -> Unit)


    suspend fun getDownloadInfo(data: E, assetIndex: Pair<Int, Int>): List<net.xzos.upgradeall.websdk.data.json.DownloadItem>
}