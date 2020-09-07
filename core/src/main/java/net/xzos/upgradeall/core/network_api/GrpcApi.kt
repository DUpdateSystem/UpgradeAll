package net.xzos.upgradeall.core.network_api

import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.Status
import io.grpc.StatusRuntimeException
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.xzos.upgradeall.core.data.config.AppConfig
import net.xzos.upgradeall.core.data.json.nongson.ObjectTag
import net.xzos.upgradeall.core.data_manager.utils.DataCache
import net.xzos.upgradeall.core.data_manager.utils.wait
import net.xzos.upgradeall.core.log.Log
import net.xzos.upgradeall.core.route.*
import java.util.concurrent.TimeUnit


object GrpcApi {

    private const val TAG = "GrpcApi"
    private val logObjectTag = ObjectTag(ObjectTag.core, TAG)
    private val invalidHubUuidList: MutableList<String> = mutableListOf()
    private var updateServerUrl: String = AppConfig.update_server_url
    private var mChannel: ManagedChannel = ManagedChannelBuilder.forTarget(updateServerUrl).usePlaintext().build()

    private const val deadlineMs = 60L
    private const val grcpWaitTime = 2000L

    private val tmpAppReleaseRequestMap = hashMapOf<String, HubData>()
    private val tmpAppReleaseRequestMapMutex = Mutex()
    private val grcpWaitLockList = hashMapOf<String, Mutex>()

    private fun HashMap<String, HubData>.getHubData(hubUuid: String): HubData {
        return runBlocking {
            tmpAppReleaseRequestMapMutex.withLock {
                this@getHubData[hubUuid] ?: HubData().also {
                    this@getHubData[hubUuid] = it
                }
            }
        }
    }

    private fun HashMap<String, HubData>.get1(hubUuid: String): HubData? {
        return runBlocking {
            tmpAppReleaseRequestMapMutex.withLock {
                this@get1[hubUuid].also {
                    this@get1.remove(hubUuid)
                }
            }
        }
    }

    private fun HashMap<String, Mutex>.setLock(hubUuid: String): Boolean {
        return if (this.containsKey(hubUuid))
            false
        else {
            this[hubUuid] = Mutex(true)
            true
        }
    }

    private fun HashMap<String, Mutex>.getLock(hubUuid: String): Mutex? {
        return this[hubUuid]
    }

    private fun HashMap<String, Mutex>.unLock(hubUuid: String): Boolean {
        val mutex = this.remove(hubUuid) ?: return false
        if (mutex.isLocked) mutex.unlock()
        return true
    }

    private fun HashMap<AppId, Mutex>.removeAppId(appId: AppId) {
        runBlocking {
            tmpAppReleaseRequestMapMutex.withLock {
                this@removeAppId.remove(appId)
            }
        }
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

    suspend fun getAppRelease(hubUuid: String, appIdMap: Map<String, String?>, auth: Map<String, String?>): List<ReleaseListItem>? {
        with(tmpAppReleaseRequestMap.getHubData(hubUuid)) {
            val appId = appIdMap.toAppId()
            addAppId(appId)
            setAuth(auth)
        }
        if (!DataCache.existsAppRelease(hubUuid, appIdMap))
            callGetAppRelease1(hubUuid)
        return if (hubUuid in invalidHubUuidList) null
        else DataCache.getAppRelease(hubUuid, appIdMap)
    }

    private suspend fun callGetAppRelease1(hubUuid: String) {
        if (grcpWaitLockList.setLock(hubUuid)) {
            delay(grcpWaitTime)
            val hubData = tmpAppReleaseRequestMap.get1(hubUuid)
            if (hubData != null)
                callGetAppRelease0(hubUuid, hubData.auth, hubData.getAppIdList())
            grcpWaitLockList.unLock(hubUuid)
        } else {
            grcpWaitLockList.getLock(hubUuid)?.wait()
        }
    }

    private suspend fun callGetAppRelease0(
            hubUuid: String, auth: Map<String, String?>, appIdList: List<AppId>
    ) {
        if (hubUuid in invalidHubUuidList) return
        val request = getReleaseRequestBuilder(hubUuid, appIdList.toList(), auth)
        val response = callGetAppRelease(request.build()) ?: return
        if (!response.validHubUuid) invalidHubUuidList.add(hubUuid)
        for (releasePackage in response.releasePackageListList) {
            val releaseList = releasePackage.releaseListList
            if (releaseList[0] == null) return
            val appId = gRPCDictToMap(releasePackage.appIdList)
            DataCache.cacheAppStatus(hubUuid, appId, releaseList)
        }
    }


    private suspend fun callGetAppRelease(request: ReleaseRequest): ReleaseResponse? {
        val blockingStub = UpdateServerRouteGrpc.newBlockingStub(mChannel)
        return try {
            blockingStub.withDeadlineAfter(deadlineMs, TimeUnit.SECONDS).getAppRelease(request)
        } catch (ignore: StatusRuntimeException) {
            if (ignore.status.code == Status.Code.DEADLINE_EXCEEDED) {
                logDeadlineError("GetAppStatus", request.hubUuid, "hub_uuid: ${request.hubUuid}, num: ${request.appIdListCount}")
            }
            null
        }
    }

    suspend fun getDownloadInfo(hubUuid: String, appId: Map<String, String?>, auth: Map<String, String?>, assetIndex: List<Int>): GetDownloadResponse? {
        if (hubUuid in invalidHubUuidList) return null
        val blockingStub = UpdateServerRouteGrpc.newBlockingStub(mChannel)
        val request = GetDownloadRequest.newBuilder()
                .setHubUuid(hubUuid)
                .addAllAppId(mapTogRPCDictTo(appId)).addAllAssetIndex(assetIndex)
                .addAllAuth(auth.map {
                    Dict.newBuilder().setK(it.key).setV(it.value).build()
                })
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

    private fun getReleaseRequestBuilder(hubUuid: String, appIdList: List<AppId>, authMap: Map<String, String?>) =
            ReleaseRequest.newBuilder()
                    .setHubUuid(hubUuid)
                    .addAllAppIdList(appIdList)
                    .addAllAuth(authMap.map {
                        Dict.newBuilder().setK(it.key).setV(it.value).build()
                    })

    private fun logDeadlineError(tag: String, hubUuid: String, appIdString: String) {
        Log.w(logObjectTag, TAG, """$tag: 请求超时，取消
                hub_uuid: $hubUuid
                app_info: $appIdString
            """.trimIndent())
    }

    private fun Map<String, String?>.toAppId() = AppId.newBuilder().addAllAppId(mapTogRPCDictTo(this)).build()
}

private class HubData {
    var auth: Map<String, String?> = mapOf()
        private set
    private val appIdList: HashSet<AppId> = hashSetOf()
    private val dataMutex = Mutex()

    fun setAuth(auth: Map<String, String?>) {
        if (this.auth != auth) this.auth = auth
    }

    fun addAppId(appId: AppId) {
        runBlocking {
            dataMutex.withLock {
                appIdList.add(appId)
            }
        }
    }

    fun getAppIdList(): List<AppId> {
        return runBlocking {
            dataMutex.withLock {
                appIdList.toList()
            }
        }
    }
}
