package net.xzos.upgradeall.core.network_api

import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.Status
import io.grpc.StatusRuntimeException
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
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
    private const val longDeadlineMs = 120L

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

    fun newClientProxy() {
        GlobalScope.launch {
            try {
                ClientProxy().newClientProxy(mChannel)
            } catch (ignore: Throwable) {
               print(ignore)
            }
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

    suspend fun getAppStatusList(hubUuid: String, appIdList: MutableList<List<AppIdItem>>): List<ResponsePackage>? {
        if (hubUuid in invalidHubUuidList) return null
        val responseList: MutableList<ResponsePackage> = mutableListOf()
        for (appId in appIdList.toList()) {
            if (DataCache.existsAppStatus(hubUuid, appId)) {
                responseList.add(ResponsePackage.newBuilder()
                        .addAllAppId(appId).setAppStatus(
                                DataCache.getAppStatus(hubUuid, appId)
                        ).build()
                )
                appIdList.remove(appId)
            }
        }
        val blockingStub = UpdateServerRouteGrpc.newBlockingStub(mChannel)
        val request = RequestList.newBuilder()
                .setHubUuid(hubUuid)
                .addAllAppIdList(appIdList.map {
                    AppId.newBuilder().addAllAppId(it).build()
                }).build()
        try {
            val responseList1 =
                    blockingStub.withDeadlineAfter(longDeadlineMs, TimeUnit.SECONDS).getAppStatusList(request).responseList
            for (responsePackage in responseList1) {
                DataCache.cacheReleaseInfo(hubUuid, responsePackage.appIdList, responsePackage.appStatus)
            }
            if (responseList.size == 1 && responseList[0].appStatus.validHubUuid) {
                invalidHubUuidList.add(hubUuid)
                return null
            }
        } catch (ignore: StatusRuntimeException) {
            if (ignore.status.code == Status.Code.DEADLINE_EXCEEDED) {
                logDeadlineError("getAppStatusList", hubUuid, appIdList.toString())
            }
            return null
        }
        return responseList
    }

    suspend fun getAppStatus(hubUuid: String, appId: List<AppIdItem>): AppStatus? {
        if (hubUuid in invalidHubUuidList) return null
        if (DataCache.existsAppStatus(hubUuid, appId))
            return DataCache.getAppStatus(hubUuid, appId)
        val blockingStub = UpdateServerRouteGrpc.newBlockingStub(mChannel)
        val request = buildRequest(hubUuid, appId)
        val returnValue = try {
            blockingStub.withDeadlineAfter(shortDeadlineMs, TimeUnit.SECONDS).getAppStatus(request)
        } catch (ignore: StatusRuntimeException) {
            if (ignore.status.code == Status.Code.DEADLINE_EXCEEDED) {
                logDeadlineError("getAppStatus", hubUuid, appId.toString())
            }
            return null
        }
        return if (!returnValue.validHubUuid) {
            invalidHubUuidList.add(hubUuid)
            null
        } else {
            DataCache.cacheReleaseInfo(hubUuid, appId, returnValue)
            returnValue
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
