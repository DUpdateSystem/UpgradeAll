package net.xzos.upgradeall.core.network_api

import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import net.xzos.upgradeall.core.data.config.AppConfig
import net.xzos.upgradeall.core.data.coroutines_basic_data_type.CoroutinesMutableMap
import net.xzos.upgradeall.core.data.coroutines_basic_data_type.coroutinesMutableListOf
import net.xzos.upgradeall.core.data.coroutines_basic_data_type.coroutinesMutableMapOf
import net.xzos.upgradeall.core.data.coroutines_basic_data_type.wait
import net.xzos.upgradeall.core.data.json.nongson.ObjectTag
import net.xzos.upgradeall.core.data_manager.utils.DataCache
import net.xzos.upgradeall.core.log.Log
import net.xzos.upgradeall.core.route.*
import java.util.concurrent.TimeUnit


@Suppress("RedundantSuspendModifier")
object GrpcApi {

    private const val TAG = "GrpcApi"
    private val logObjectTag = ObjectTag(ObjectTag.core, TAG)
    private val invalidHubUuidList = hashSetOf<String>()
    private var updateServerUrl: String = AppConfig.update_server_url
    private var mChannel: ManagedChannel = ManagedChannelBuilder.forTarget(updateServerUrl).usePlaintext().build()

    private const val deadlineMs = 60L
    private const val grpcWaitTime = 1000L

    private val hubDataMap = coroutinesMutableMapOf<String, HubData>()
    private val grpcWaitLockList = coroutinesMutableListOf<String>(true)
    private val grpcAppItemWaitLockList = coroutinesMutableMapOf<String, Mutex>(true)

    private suspend fun CoroutinesMutableMap<String, HubData>.getHubData(hubUuid: String, auth: Map<String, String?>): HubData {
        val hubKey = (hubUuid + auth).md5()
        return this@getHubData[hubKey] ?: HubData(hubUuid, auth).also {
            this@getHubData[hubKey] = it
        }
    }

    private suspend fun CoroutinesMutableMap<String, HubData>.popHubData(hubKey: String): HubData? {
        return this@popHubData.remove(hubKey)
    }

    private fun CoroutinesMutableMap<String, Mutex>.setLock(key: String, locked: Boolean = false): Mutex? {
        return if (this.containsKey(key))
            null
        else {
            Mutex(locked).also {
                this[key] = it
            }
        }
    }

    private fun CoroutinesMutableMap<String, Mutex>.getLock(key: String): Mutex? {
        return this[key]
    }

    private fun CoroutinesMutableMap<String, Mutex>.unLock(key: String): Boolean {
        val mutex = this.remove(key) ?: return false
        if (mutex.isLocked) mutex.unlock()
        return true
    }

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
            blockingStub.withDeadlineAfter(deadlineMs, TimeUnit.SECONDS).getCloudConfig(Empty.newBuilder().build()).s
        } catch (ignore: StatusRuntimeException) {
            null
        }
    }

    suspend fun getAppRelease(hubUuid: String, auth: Map<String, String?>, appId: Map<String, String?>): List<ReleaseListItem>? {
        if (hubUuid in invalidHubUuidList) return null
        if (!DataCache.existsAppRelease(hubUuid, auth, appId)) {
            val itemKey = (hubUuid + auth + appId).md5()
            if (grpcAppItemWaitLockList.setLock(itemKey, true) != null) {
                with(hubDataMap.getHubData(hubUuid, auth)) {
                    addAppId(appId)
                }
                val hubKey = (hubUuid + auth).md5()
                callGetAppReleaseStream(hubKey)
            }
            grpcAppItemWaitLockList.getLock(itemKey)?.wait()
        }
        return DataCache.getAppRelease(hubUuid, auth, appId)
    }

    private suspend fun callGetAppReleaseStream(hubKey: String) {
        grpcWaitLockList.add(hubKey).let {
            if (it) {
                delay(grpcWaitTime)
                callGetAppReleaseStream0(hubKey)
                grpcWaitLockList.remove(hubKey)
            }
        }
    }

    private suspend fun callGetAppReleaseStream0(hubKey: String) {
        val hubData = hubDataMap.popHubData(hubKey) ?: return
        val hubUuid = hubData.hubUuid
        val auth = hubData.auth
        val appIdList = hubData.getAppIdList()
        val responseObserver: StreamObserver<ReleaseResponse> = object : StreamObserver<ReleaseResponse> {
            override fun onNext(response: ReleaseResponse) {
                if (response.validHub) {
                    val releasePackage = response.release
                    val appId = releasePackage.appIdList.toMap()
                    if (releasePackage.validData)
                        DataCache.cacheAppStatus(hubUuid, auth, appId, releasePackage.releaseListList)
                    val itemKey = (hubUuid + auth + appId).md5()
                    grpcAppItemWaitLockList.unLock(itemKey)
                } else
                    invalidHubUuidList.add(hubUuid)
            }

            override fun onError(t: Throwable) {
                for (appId in appIdList) {
                    val itemKey = (hubUuid + auth + appId).md5()
                    grpcAppItemWaitLockList.unLock(itemKey)
                }
            }

            override fun onCompleted() {}
        }
        val request = getReleaseRequestBuilder(hubUuid, appIdList.toList(), auth)
        callGetAppRelease(request.build(), responseObserver)
    }


    private fun callGetAppRelease(request: ReleaseRequest, responseObserver: StreamObserver<ReleaseResponse>) {
        val asyncStub = UpdateServerRouteGrpc.newStub(mChannel)
        try {
            asyncStub.getAppRelease(request, responseObserver)
        } catch (ignore: StatusRuntimeException) {
            if (ignore.status.code == Status.Code.DEADLINE_EXCEEDED) {
                logDeadlineError("CallGetAppRelease", request.hubUuid, "hub_uuid: ${request.hubUuid}, num: ${request.appIdListCount}")
            }
        }
    }

    suspend fun getDownloadInfo(hubUuid: String, appId: Map<String, String?>, auth: Map<String, String?>, assetIndex: List<Int>): GetDownloadResponse? {
        if (hubUuid in invalidHubUuidList) return null
        val blockingStub = UpdateServerRouteGrpc.newBlockingStub(mChannel)
        val request = GetDownloadRequest.newBuilder()
                .setHubUuid(hubUuid)
                .addAllAppId(appId.togRPCDict()).addAllAssetIndex(assetIndex)
                .addAllAuth(auth.togRPCDict())
                .build()
        return try {
            blockingStub.withDeadlineAfter(deadlineMs, TimeUnit.SECONDS).devGetDownloadInfo(request)
        } catch (ignore: StatusRuntimeException) {
            if (ignore.status.code == Status.Code.DEADLINE_EXCEEDED) {
                logDeadlineError("GetDownloadInfo", hubUuid, appId.toString())
            }
            null
        }
    }

    private fun getReleaseRequestBuilder(hubUuid: String, appIdList: List<Map<String, String?>>, authMap: Map<String, String?>) =
            ReleaseRequest.newBuilder()
                    .setHubUuid(hubUuid)
                    .addAllAppIdList(appIdList.map { AppId.newBuilder().addAllAppId(it.togRPCDict()).build() })
                    .addAllAuth(authMap.map {
                        Dict.newBuilder().setK(it.key).setV(it.value).build()
                    })

    private fun logDeadlineError(tag: String, hubUuid: String, appIdString: String) {
        Log.w(logObjectTag, TAG, """$tag: 请求超时，取消
                hub_uuid: $hubUuid
                app_info: $appIdString
            """.trimIndent())
    }
}
