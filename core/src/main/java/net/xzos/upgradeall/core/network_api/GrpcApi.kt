package net.xzos.upgradeall.core.network_api

import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.Status
import io.grpc.StatusRuntimeException
import net.xzos.upgradeall.core.data.config.AppConfig
import net.xzos.upgradeall.core.data.config.AppValue.def_update_server_url
import net.xzos.upgradeall.core.data.json.nongson.ObjectTag
import net.xzos.upgradeall.core.data_manager.utils.DataCache
import net.xzos.upgradeall.core.log.Log
import net.xzos.upgradeall.core.route.*
import java.net.URISyntaxException
import java.util.concurrent.TimeUnit

object GrpcApi {
    internal const val TAG = "GrpcApi"
    internal val logObjectTag = ObjectTag(ObjectTag.core, TAG)
    val invalidHubUuidList = hashSetOf<String>()

    private val updateServerUrl get() = AppConfig.update_server_url
    fun getChannel(): ManagedChannel? {
        return try {
            ManagedChannelBuilder.forTarget(updateServerUrl).usePlaintext().build()
        } catch (e: URISyntaxException) {
            Log.e(logObjectTag, TAG, "gRPC 接口地址格式有误，$e")
            ManagedChannelBuilder.forTarget(def_update_server_url).usePlaintext().build()
        } catch (e: Throwable) {
            Log.e(logObjectTag, TAG, "gRPC 初始化失败，$e")
            null
        }
    }

    const val deadlineMs = 20 * 1000L
    fun logDeadlineError(tag: String, hubUuid: String, appIdString: String) {
        Log.w(logObjectTag, TAG, """$tag: 请求超时，取消
hub_uuid: $hubUuid
$appIdString""".trimIndent())
    }

    @Suppress("RedundantSuspendModifier")
    suspend fun getCloudConfig(): String? {
        val channel = getChannel() ?: return null
        val blockingStub = UpdateServerRouteGrpc.newBlockingStub(channel)
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
                    ?: GrpcReleaseApi.getAppRelease(hubUuid, auth, appId)?.also {
                        DataCache.cacheAppStatus(hubUuid, auth, appId, it)
                    }
        }
    }

    @Suppress("RedundantSuspendModifier")
    suspend fun getDownloadInfo(hubUuid: String, appId: Map<String, String?>, auth: Map<String, String?>, assetIndex: List<Int>): GetDownloadResponse? {
        if (hubUuid in invalidHubUuidList) return null
        val channel = getChannel() ?: return null
        val blockingStub = UpdateServerRouteGrpc.newBlockingStub(channel)
        val request = GetDownloadRequest.newBuilder()
                .setHubUuid(hubUuid)
                .addAllAppId(appId.togRPCDict()).addAllAssetIndex(assetIndex)
                .addAllAuth(auth.togRPCDict())
                .build()
        return try {
            blockingStub.withDeadlineAfter(deadlineMs * 2, TimeUnit.MILLISECONDS).devGetDownloadInfo(request)
        } catch (ignore: StatusRuntimeException) {
            if (ignore.status.code == Status.Code.DEADLINE_EXCEEDED) {
                logDeadlineError("GetDownloadInfo", hubUuid, appId.toString())
            }
            null
        }
    }
}
