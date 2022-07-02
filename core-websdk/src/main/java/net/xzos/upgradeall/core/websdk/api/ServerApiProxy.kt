package net.xzos.upgradeall.core.websdk.api

import net.xzos.upgradeall.core.utils.coroutines.coroutinesMutableListOf
import net.xzos.upgradeall.core.websdk.base_model.ApiRequestData
import net.xzos.upgradeall.core.websdk.json.DownloadItem
import net.xzos.upgradeall.core.websdk.json.ReleaseGson

class ServerApiProxy internal constructor(
    private val getServerApi: () -> ServerApi?
) {
    private val serverApi get() = getServerApi()
    private val requestDataList = coroutinesMutableListOf<ApiRequestData>()

    fun getAppRelease(requestData: ApiRequestData, callback: (List<ReleaseGson>?) -> Unit) {
        serverApi?.also { api ->
            requestDataList.add(requestData)
            api.getAppRelease(requestData) {
                requestDataList.remove(requestData)
                callback(it)
            }
        } ?: callback(null)
    }

    fun getAppReleaseList(requestData: ApiRequestData, callback: (List<ReleaseGson>?) -> Unit) {
        serverApi?.also { api ->
            requestDataList.add(requestData)
            api.getAppReleaseList(requestData) {
                requestDataList.remove(requestData)
                callback(it)
            }
        } ?: callback(null)
    }

    suspend fun getDownloadInfo(
        requestData: ApiRequestData,
        assetIndex: List<Int>
    ): List<DownloadItem> {
        return serverApi?.let {
            requestDataList.add(requestData)
            it.getDownloadInfo(requestData, Pair(assetIndex.first(), assetIndex.last())).apply {
                requestDataList.remove(requestData)
            }
        } ?: emptyList()
    }

    fun cancel() {
        requestDataList.forEach {
            serverApi?.cancelRequest(it)
        }
    }
}