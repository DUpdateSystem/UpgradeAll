package net.xzos.upgradeall.core.network_api

import android.os.Looper
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.Status
import io.grpc.StatusRuntimeException
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import net.xzos.upgradeall.core.data.config.AppConfig
import net.xzos.upgradeall.core.data.coroutines.CoroutinesMutableMap
import net.xzos.upgradeall.core.data.coroutines.coroutinesMutableListOf
import net.xzos.upgradeall.core.data.coroutines.coroutinesMutableMapOf
import net.xzos.upgradeall.core.data.json.nongson.ObjectTag
import net.xzos.upgradeall.core.data_manager.utils.DataCache
import net.xzos.upgradeall.core.log.Log
import net.xzos.upgradeall.core.route.*
import net.xzos.upgradeall.core.utils.wait
import java.util.concurrent.TimeUnit

class GrpcApi {

    private val hubDataMap = coroutinesMutableMapOf<String, HubData>(true)
    private val grpcWaitLockList = coroutinesMutableListOf<String>(true)
    private val grpcAppItemWaitLockList = coroutinesMutableMapOf<String, Mutex>(true)

    private fun CoroutinesMutableMap<String, HubData>.getHubData(hubUuid: String, auth: Map<String, String?>): HubData {
        val hubKey = mkHubId(hubUuid, auth)
        return this@getHubData[hubKey] ?: HubData(hubUuid, auth).also {
            this@getHubData[hubKey] = it
        }
    }

    private fun CoroutinesMutableMap<String, HubData>.popHubData(hubKey: String): HubData? {
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

    suspend fun getAppRelease(hubUuid: String, auth: Map<String, String?>, appId: Map<String, String?>): List<ReleaseListItem>? {
        if (hubUuid in invalidHubUuidList) return null
        return DataCache.getAppRelease(hubUuid, auth, appId) ?: kotlin.run {
            if (Looper.myLooper() == Looper.getMainLooper()) return null
            val itemKey = mkAppId(hubUuid, auth, appId)
            if (grpcAppItemWaitLockList.setLock(itemKey, true) != null) {
                with(hubDataMap.getHubData(hubUuid, auth)) {
                    addAppId(appId)
                }
                GlobalScope.launch {
                    callGetAppReleaseStream(mkHubId(hubUuid, auth))
                }
            }
            try {
                grpcAppItemWaitLockList.getLock(itemKey)?.wait()
                DataCache.getAppRelease(hubUuid, auth, appId)
            } catch (ignore: TimeoutCancellationException) {
                grpcAppItemWaitLockList.unLock(itemKey)
                null
            }
        }
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
        for (appIdL in appIdList.chunked(50))
            callGetAppRelease(hubUuid, auth, appIdL.toHashSet())
    }

    private suspend fun callGetAppRelease(
            hubUuid: String, auth: Map<String, String?>, appIdList: HashSet<Map<String, String?>>
    ) {
        val request = getReleaseRequestBuilder(hubUuid, appIdList, auth)
        val clearMutex = fun() {
            for (appId in appIdList.toList()) {
                grpcAppItemWaitLockList.unLock(mkAppId(hubUuid, auth, appId))
            }
        }

        val asyncStub = UpdateServerRouteGrpc.newBlockingStub(mChannel)
        try {
            val releaseResponseIterator = asyncStub.withDeadlineAfter(deadlineMs, TimeUnit.MILLISECONDS).getAppRelease(request.build())
            var response: ReleaseResponse
            while (withTimeout(deadlineMs) { releaseResponseIterator.next().also { response = it } } != null) {
                if (response.validHub) {
                    val releasePackage = response.release
                    val appId = releasePackage.appIdList.toMap()
                    if (releasePackage.validData)
                        DataCache.cacheAppStatus(hubUuid, auth, appId, releasePackage.releaseListList)
                    val itemKey = mkAppId(hubUuid, auth, appId)
                    grpcAppItemWaitLockList.unLock(itemKey)
                    appIdList.remove(appId)
                } else {
                    invalidHubUuidList.add(hubUuid)
                }
            }
        } catch (e: Throwable) {
            if ((e is StatusRuntimeException && e.status.code == Status.Code.DEADLINE_EXCEEDED)
                    || e is TimeoutCancellationException) {
                logDeadlineError("CallGetAppRelease", request.hubUuid, "hub_uuid: ${hubUuid}, num: ${request.appIdListCount}, cancel: ${appIdList.size}, error:$e")
            }
        } finally {
            clearMutex()
        }
    }

    private fun mkAppId(hubUuid: String, auth: Map<String, String?>, appId: Map<String, String?>) = (hubUuid + auth + appId).md5()

    private fun mkHubId(hubUuid: String, auth: Map<String, String?>) = (hubUuid + auth).md5()

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

    private fun getReleaseRequestBuilder(hubUuid: String, appIdList: HashSet<Map<String, String?>>, authMap: Map<String, String?>) =
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

    companion object {
        val grpcApi by lazy { GrpcApi() }

        private const val TAG = "GrpcApi"
        private val logObjectTag = ObjectTag(ObjectTag.core, TAG)
        private val invalidHubUuidList = hashSetOf<String>()

        private var updateServerUrl: String = AppConfig.update_server_url
        private var mChannel: ManagedChannel = ManagedChannelBuilder.forTarget(updateServerUrl).usePlaintext().build()


        private const val deadlineMs = 10 * 1000L
        private const val grpcWaitTime = 1000L
    }
}
