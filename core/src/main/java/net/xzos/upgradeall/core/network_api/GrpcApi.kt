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

class GrpcApi {

    private val grpcReleaseApi = GrpcReleaseApi()

    internal fun setUpdateServerUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        return try {
            mChannel.shutdownNow().awaitTermination(5, TimeUnit.MILLISECONDS)
            mChannel = ManagedChannelBuilder.forTarget(url).usePlaintext().build()
            true
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    @Suppress("RedundantSuspendModifier")
    suspend fun getCloudConfig(): String? {
        val blockingStub = UpdateServerRouteGrpc.newBlockingStub(mChannel)
        return try {
            blockingStub.withDeadlineAfter(deadlineMs, TimeUnit.MILLISECONDS).getCloudConfig(Empty.newBuilder().build()).s
        } catch (ignore: StatusRuntimeException) {
            null
        }
    }

    suspend fun getAppRelease(
            hubUuid: String, auth: Map<String, String?>, appId: Map<String, String?>): List<ReleaseListItem>? {
        return if (hubUuid in invalidHubUuidList) {
            null
        } else {
            DataCache.getAppRelease(hubUuid, auth, appId)
                    ?: grpcReleaseApi.getAppRelease(hubUuid, auth, appId).also {
                        DataCache.cacheAppStatus(hubUuid, auth, appId, it)
                    }
        }
    }

    @Suppress("RedundantSuspendModifier")
    suspend fun getDownloadInfo(hubUuid: String, appId: Map<String, String?>, auth: Map<String, String?>, assetIndex: List<Int>): GetDownloadResponse? {
        if (hubUuid in invalidHubUuidList) return null
        val blockingStub = UpdateServerRouteGrpc.newBlockingStub(mChannel)
        val request = GetDownloadRequest.newBuilder()
                .setHubUuid(hubUuid)
                .addAllAppId(appId.togRPCDict()).addAllAssetIndex(assetIndex)
                .addAllAuth(auth.togRPCDict())
                .build()
        return try {
            blockingStub.withDeadlineAfter(deadlineMs, TimeUnit.MILLISECONDS).devGetDownloadInfo(request)
        } catch (ignore: StatusRuntimeException) {
            if (ignore.status.code == Status.Code.DEADLINE_EXCEEDED) {
                logDeadlineError("GetDownloadInfo", hubUuid, appId.toString())
            }
            null
        }
    }

    companion object {
        val grpcApi by lazy { GrpcApi() }

        private const val TAG = "GrpcApi"
        private val logObjectTag = ObjectTag(ObjectTag.core, TAG)
        val invalidHubUuidList = hashSetOf<String>()

        private var updateServerUrl: String = AppConfig.update_server_url
        var mChannel: ManagedChannel = ManagedChannelBuilder.forTarget(updateServerUrl).usePlaintext().build()

        const val deadlineMs = 10 * 1000L
        fun logDeadlineError(tag: String, hubUuid: String, appIdString: String) {
            Log.w(logObjectTag, TAG, """$tag: 请求超时，取消
                hub_uuid: $hubUuid
                app_info: $appIdString
            """.trimIndent())
        }
    }
}
