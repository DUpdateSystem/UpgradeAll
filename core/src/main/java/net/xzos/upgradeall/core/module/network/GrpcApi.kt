package net.xzos.upgradeall.core.module.network

import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.Status
import io.grpc.StatusRuntimeException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.xzos.upgradeall.core.coreConfig
import net.xzos.upgradeall.core.data.DEF_UPDATE_SERVER_URL
import net.xzos.upgradeall.core.log.Log
import net.xzos.upgradeall.core.log.ObjectTag
import net.xzos.upgradeall.core.log.ObjectTag.Companion.core
import net.xzos.upgradeall.core.log.errorToString
import net.xzos.upgradeall.core.route.*
import net.xzos.upgradeall.core.utils.coroutines.CoroutinesMutableList
import java.net.URISyntaxException
import java.util.concurrent.TimeUnit

internal object GrpcApi {
    internal const val TAG = "GrpcApi"
    internal val logObjectTag = ObjectTag(core, TAG)
    val invalidHubUuidList = hashSetOf<String>()

    private val updateServerUrl get() = coreConfig.update_server_url
    private val channelList = CoroutinesMutableList<ManagedChannel>(true)
    private val getterMutex = Mutex()

    suspend fun getChannel(): ManagedChannel? {
        return getterMutex.withLock {
            if (channelList.size >= 3) {
                channelList.random()
            } else {
                val channel = try {
                    ManagedChannelBuilder.forTarget(updateServerUrl).usePlaintext().build()
                } catch (e: URISyntaxException) {
                    Log.e(logObjectTag, TAG, "gRPC 接口地址格式有误，$e")
                    ManagedChannelBuilder.forTarget(DEF_UPDATE_SERVER_URL).usePlaintext().build()
                } catch (e: Throwable) {
                    Log.e(logObjectTag, TAG, "gRPC 初始化失败，$e")
                    null
                }
                channel?.apply {
                    channelList.add(this)
                }
            }
        }
    }

    const val deadlineMs = 20 * 1000L
    fun logDeadlineError(tag: String, hubUuid: String, appIdString: String) {
        Log.w(
                logObjectTag, TAG, """$tag: 请求超时，取消
hub_uuid: $hubUuid
$appIdString""".trimIndent()
        )
    }

    @Suppress("RedundantSuspendModifier")
    suspend fun getCloudConfig(): String? {
        val channel = getChannel() ?: return null
        val blockingStub = UpdateServerRouteGrpc.newBlockingStub(channel)
        return try {
            blockingStub.withDeadlineAfter(deadlineMs, TimeUnit.MILLISECONDS)
                    .getCloudConfig(Str.newBuilder().setS("dev").build()).s
        } catch (e: StatusRuntimeException) {
            Log.e(logObjectTag, TAG, "gRPC CloudConfig 获取失败，${errorToString(e)}")
            null
        }
    }

    suspend fun getAppRelease(
            hubUuid: String, auth: Map<String, String?>, appId: Map<String, String?>, priority: Int
    ): List<ReleaseListItem>? {
        return if (hubUuid in invalidHubUuidList) {
            null
        } else {
            DataCache.getAppRelease(hubUuid, auth, appId)
                    ?: GrpcReleaseApi.getAppRelease(hubUuid, auth, appId, priority)?.also {
                        DataCache.cacheAppStatus(hubUuid, auth, appId, it)
                    }
        }
    }

    @Suppress("RedundantSuspendModifier")
    suspend fun getDownloadInfo(
            hubUuid: String,
            appId: Map<String, String?>,
            auth: Map<String, String?>,
            assetIndex: Pair<Int, Int>
    ): GetDownloadResponse? {
        if (hubUuid in invalidHubUuidList) return null
        val channel = getChannel() ?: return null
        val blockingStub = UpdateServerRouteGrpc.newBlockingStub(channel)
        val request = GetDownloadRequest.newBuilder()
                .setHubUuid(hubUuid)
                .addAllAppId(appId.togRPCDict()).addAllAssetIndex(assetIndex.toList())
                .addAllAuth(auth.togRPCDict())
                .build()
        return try {
            blockingStub.withDeadlineAfter(deadlineMs * 2, TimeUnit.MILLISECONDS)
                    .devGetDownloadInfo(request)
        } catch (ignore: StatusRuntimeException) {
            if (ignore.status.code == Status.Code.DEADLINE_EXCEEDED) {
                logDeadlineError("GetDownloadInfo", hubUuid, appId.toString())
            }
            null
        }
    }
}
