package net.xzos.upgradeall.core.module.network

import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.Status
import io.grpc.StatusRuntimeException
import net.xzos.upgradeall.core.coreConfig
import net.xzos.upgradeall.core.data.DEF_UPDATE_SERVER_URL
import net.xzos.upgradeall.core.log.Log
import net.xzos.upgradeall.core.log.ObjectTag
import net.xzos.upgradeall.core.log.ObjectTag.Companion.core
import net.xzos.upgradeall.core.log.msg
import net.xzos.upgradeall.core.route.*
import net.xzos.upgradeall.core.utils.coroutines.CoroutinesMutableList
import java.net.URISyntaxException
import java.util.concurrent.TimeUnit

internal object GrpcApi {
    internal const val TAG = "GrpcApi"
    internal val logObjectTag = ObjectTag(core, TAG)

    const val deadlineMs = 15 * 1000L
    val invalidHubUuidList = hashSetOf<String>()

    private val updateServerUrl get() = coreConfig.update_server_url
    private val stubList = CoroutinesMutableList<UpdateServerRouteGrpc.UpdateServerRouteStub>(true)
    private var channel: ManagedChannel? = newChannel()

    fun renewChannel() {
        channel?.shutdown()
        stubList.clear()
        channel = newChannel()
    }

    fun getStub(): UpdateServerRouteGrpc.UpdateServerRouteStub? {
        return if (stubList.size >= 3)
            stubList.random()
        else
            UpdateServerRouteGrpc.newStub(channel ?: return null)?.apply {
                stubList.add(this)
            }
    }

    fun getDeadlineMs(num: Int): Long {
        return when {
            num <= 3 -> deadlineMs
            num <= 20 -> deadlineMs * num
            else -> 600000L  // 10min
        }
    }

    fun removeStub(stub: UpdateServerRouteGrpc.UpdateServerRouteStub) {
        stubList.remove(stub)
    }

    private fun newChannel(): ManagedChannel? {
        return try {
            ManagedChannelBuilder.forTarget(updateServerUrl).usePlaintext().build()
        } catch (e: URISyntaxException) {
            Log.e(logObjectTag, TAG, "gRPC 接口地址格式有误，${e.msg()}")
            ManagedChannelBuilder.forTarget(DEF_UPDATE_SERVER_URL).usePlaintext().build()
        } catch (e: Throwable) {
            Log.e(logObjectTag, TAG, "gRPC 初始化失败，${e.msg()}")
            null
        }
    }

    fun logDeadlineError(tag: String, hubUuid: String, appIdString: String) {
        Log.w(
            logObjectTag, TAG, """$tag: 请求超时，取消
hub_uuid: $hubUuid
$appIdString""".trimIndent()
        )
    }

    @Suppress("RedundantSuspendModifier")
    suspend fun getCloudConfig(): String? {
        val blockingStub = UpdateServerRouteGrpc.newBlockingStub(channel ?: return null)
        return try {
            blockingStub.withDeadlineAfter(deadlineMs, TimeUnit.MILLISECONDS)
                .getCloudConfig(Str.newBuilder().setS("dev").build()).s
        } catch (e: StatusRuntimeException) {
            Log.e(logObjectTag, TAG, "gRPC CloudConfig 获取失败，${e.msg()}")
            null
        }
    }

    fun getAppRelease(
        hubUuid: String, auth: Map<String, String?>, appId: Map<String, String?>, priority: Int,
        callback: (List<ReleaseListItem>?) -> Unit
    ) {
        if (hubUuid in invalidHubUuidList) {
            callback(null)
        } else {
            DataCache.getAppRelease(hubUuid, auth, appId)?.also {
                callback(it)
            } ?: GrpcReleaseApi.getAppRelease(hubUuid, auth, appId, priority) {
                it?.let {
                    DataCache.cacheAppStatus(hubUuid, auth, appId, it)
                }
                callback(it)
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
        val blockingStub = UpdateServerRouteGrpc.newBlockingStub(channel ?: return null)
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
