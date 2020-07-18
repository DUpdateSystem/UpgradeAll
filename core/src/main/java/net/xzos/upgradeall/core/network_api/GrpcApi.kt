package net.xzos.upgradeall.core.network_api

import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.Status
import io.grpc.StatusRuntimeException
import net.xzos.upgradeall.core.data.config.AppConfig
import net.xzos.upgradeall.core.data.json.nongson.ObjectTag
import net.xzos.upgradeall.core.data_manager.utils.DataCache
import net.xzos.upgradeall.core.log.Log
import net.xzos.upgradeall.core.route.*
import java.util.concurrent.TimeUnit


object GrpcApi {

    private const val TAG = "GrpcApi"
    private val logObjectTag = ObjectTag(ObjectTag.core, TAG)
    private val invalidHubUuidList: MutableList<String> = mutableListOf()
    private var updateServerUrl: String = AppConfig.update_server_url
    private var mChannel: ManagedChannel = ManagedChannelBuilder.forTarget(updateServerUrl).usePlaintext().build()

    private const val shortDeadlineMs = 15L

    internal fun setUpdateServerUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        return try {
            mChannel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS)
            mChannel = ManagedChannelBuilder.forTarget(url).usePlaintext().build()
            true
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    suspend fun getCloudConfig(): String? {
        val blockingStub = UpdateServerRouteGrpc.newBlockingStub(mChannel)
        return try {
            blockingStub.withDeadlineAfter(shortDeadlineMs, TimeUnit.SECONDS).getCloudConfig(Empty.newBuilder().build()).s
        } catch (ignore: StatusRuntimeException) {
            null
        }
    }

    suspend fun getAppStatus(hubUuid: String, appId: List<AppIdItem>): AppStatus? {
        if (hubUuid in invalidHubUuidList) return null
        if (DataCache.existsAppStatus(hubUuid, appId))
            return DataCache.getAppStatus(hubUuid, appId)
        var request = buildRequest(hubUuid, appId)
        var response: Response?
        do {
            response = callGetAppStatus(request)
            if (response == null) break
            else if (response.needProxy) {
                request = ClientProxy.processRequest(hubUuid, appId, response)
            }
        } while (response?.needProxy == true)
        val appStatus = response?.appStatus ?: return null
        if (!appStatus.validHubUuid) {
            invalidHubUuidList.add(hubUuid)
            return null
        } else {
            DataCache.cacheReleaseInfo(hubUuid, appId, appStatus)
        }
        return appStatus
    }


    private suspend fun callGetAppStatus(request: Request): Response? {
        val blockingStub = UpdateServerRouteGrpc.newBlockingStub(mChannel)
        return try {
            blockingStub.withDeadlineAfter(shortDeadlineMs, TimeUnit.SECONDS).getAppStatus(request)
        } catch (ignore: StatusRuntimeException) {
            if (ignore.status.code == Status.Code.DEADLINE_EXCEEDED) {
                logDeadlineError("getAppStatus", request.hubUuid, request.appIdList.toString())
            }
            null
        }
    }

    suspend fun getDownloadInfo(hubUuid: String, appId: List<AppIdItem>, assetIndex: List<Int>): DownloadInfo? {
        if (hubUuid in invalidHubUuidList) return null
        val blockingStub = UpdateServerRouteGrpc.newBlockingStub(mChannel)
        val request = DownloadAssetIndex.newBuilder().setAppIdInfo(
                buildRequest(hubUuid, appId)
        ).apply {
            for (i in assetIndex)
                addAssetIndex(i)
        }.build()
        return try {
            blockingStub.withDeadlineAfter(shortDeadlineMs, TimeUnit.SECONDS).getDownloadInfo(request)
        } catch (ignore: StatusRuntimeException) {
            if (ignore.status.code == Status.Code.DEADLINE_EXCEEDED) {
                logDeadlineError("getDownloadInfo", hubUuid, appId.toString())
            }
            null
        }
    }

    private fun buildRequest(hubUuid: String, appId: List<AppIdItem>) =
            Request.newBuilder().setHubUuid(hubUuid).addAllAppId(appId).build()

    private fun logDeadlineError(tag: String, hubUuid: String, appIdString: String) {
        Log.w(logObjectTag, TAG, """$tag: 请求超时，取消
                hub_uuid: $hubUuid
                app_info: $appIdString
            """.trimIndent())
    }
}
