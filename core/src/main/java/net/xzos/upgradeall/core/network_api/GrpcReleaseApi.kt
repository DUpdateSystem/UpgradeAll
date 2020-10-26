package net.xzos.upgradeall.core.network_api

import io.grpc.ManagedChannel
import io.grpc.Status
import io.grpc.StatusRuntimeException
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.xzos.upgradeall.core.data.coroutines.CoroutinesMutableList
import net.xzos.upgradeall.core.data.coroutines.CoroutinesMutableMap
import net.xzos.upgradeall.core.data.coroutines.coroutinesMutableListOf
import net.xzos.upgradeall.core.data.coroutines.coroutinesMutableMapOf
import net.xzos.upgradeall.core.log.Log
import net.xzos.upgradeall.core.route.*
import net.xzos.upgradeall.core.utils.wait

object GrpcReleaseApi {
    private const val grpcWaitTime = 200L
    private var chunkedSize = 200

    private val funMap: CoroutinesMutableMap<String, CoroutinesMutableList<(_: List<ReleaseListItem>?) -> Unit>> = coroutinesMutableMapOf(true)
    private val hubDataMap = coroutinesMutableMapOf<String, HubData>(true)
    private val grpcWaitLockList = coroutinesMutableListOf<String>(true)
    private val mutex = Mutex()
    private val grpcRequestLockMap = coroutinesMutableMapOf<String, Mutex>(true)

    suspend fun getAppRelease(hubUuid: String, auth: Map<String, String?>, appId: Map<String, String?>): List<ReleaseListItem>? {
        val itemKey = mkAppId(hubUuid, auth, appId)
        val mutex = grpcRequestLockMap[itemKey] ?: Mutex(true).apply {
            grpcRequestLockMap[itemKey] = this
            setRequest(hubUuid, auth, appId, fun(_) {
                grpcRequestLockMap.remove(itemKey)?.unlock()
            })
        }
        var releaseList: List<ReleaseListItem>? = null
        setRequest(hubUuid, auth, appId, fun(list) {
            releaseList = list
        })
        GlobalScope.launch {
            callGetAppReleaseStream(mkHubId(hubUuid, auth))
        }
        mutex.wait()
        return releaseList
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
        chunkedCallGetAppRelease(hubUuid, auth, appIdList)
    }

    private suspend fun chunkedCallGetAppRelease(
            hubUuid: String, auth: Map<String, String?>,
            appIdList: Collection<Map<String, String?>>, autoRetryNum: Int = 3,
    ) {
        val channel = GrpcApi.getChannel()
        appIdList.chunked(chunkedSize).forEach { chunkedList ->
            GlobalScope.launch {
                callGetAppRelease(hubUuid, auth, chunkedList, autoRetryNum, channel)
            }
        }
    }

    internal suspend fun callGetAppRelease(
            hubUuid: String, auth: Map<String, String?>,
            appIdList0: Collection<Map<String, String?>>, autoRetryNum: Int,
            channel: ManagedChannel = GrpcApi.getChannel(),
    ) {
        val appIdList = appIdList0.toMutableList()
        val request = mkReleaseRequestBuilder(hubUuid, appIdList, auth)
        val clearMutex = fun() {
            val size = appIdList.size
            if (size > 0) {
                Log.w(GrpcApi.logObjectTag, GrpcApi.TAG, "callGetAppRelease: 放弃请求 hub_uuid: $hubUuid num: $size")
                for (appId in appIdList.toList()) {
                    callRequestFun(hubUuid, auth, appId, null)
                }
            }
        }
        if (hubUuid in GrpcApi.invalidHubUuidList) {
            clearMutex()
            return
        }
        try {
            val blockingStub = UpdateServerRouteGrpc.newBlockingStub(channel)
            val releaseResponseIterator = blockingStub.getAppRelease(request.build())
            var firstIndex = true
            while (withTimeout(GrpcApi.deadlineMs * 3) {
                        releaseResponseIterator.hasNext()
                    }
            ) {
                val response = releaseResponseIterator.next()
                if (firstIndex) {
                    if (!response.validHub) {
                        GrpcApi.invalidHubUuidList.add(hubUuid)
                        break
                    }
                }
                firstIndex = false
                val releasePackage = response.release
                val appId = releasePackage.appIdList.toMap()
                val releaseList = if (releasePackage.validData)
                    releasePackage.releaseListList
                else null
                callRequestFun(hubUuid, auth, appId, releaseList)
                appIdList.remove(appId)
            }
        } catch (e: Throwable) {
            if ((e is StatusRuntimeException && e.status.code == Status.Code.DEADLINE_EXCEEDED)
                    || e is TimeoutCancellationException) {
                GrpcApi.logDeadlineError("CallGetAppRelease", request.hubUuid,
                        "hub_uuid: ${hubUuid}, num: ${request.appIdListCount}, cancel: ${appIdList.size}, error:$e")
                if (chunkedSize > 25) {
                    chunkedSize = (chunkedSize / 1.2).toInt()
                }
            } else {
                Log.w(GrpcApi.logObjectTag, GrpcApi.TAG, "callGetAppRelease: 请求失败 hub_uuid: $hubUuid e: $e".trimIndent())
            }
        } finally {
            val size = appIdList.size
            if (size > 0) {
                if (autoRetryNum > 0) {
                    Log.w(GrpcApi.logObjectTag, GrpcApi.TAG, "callGetAppRelease: 重新请求 hub_uuid: $hubUuid num: $size")
                    chunkedCallGetAppRelease(hubUuid, auth, appIdList, autoRetryNum - 1)
                } else {
                    clearMutex()
                }
            }
        }
    }

    private fun callRequestFun(hubUuid: String, auth: Map<String, String?>, appId: Map<String, String?>, releaseList: List<ReleaseListItem>?) {
        val itemId = mkAppId(hubUuid, auth, appId)
        funMap.remove(itemId)?.map { func ->
            func(releaseList)
        }
    }

    internal suspend fun setRequest(hubUuid: String, auth: Map<String, String?>, appId: Map<String, String?>, func: (_: List<ReleaseListItem>?) -> Unit) {
        mutex.withLock {
            val itemKey = mkAppId(hubUuid, auth, appId)
            hubDataMap.getHubData(hubUuid, auth).addAppId(appId)
            val funList = funMap[itemKey]
                    ?: coroutinesMutableListOf<(_: List<ReleaseListItem>?) -> Unit>().also {
                        funMap[itemKey] = it
                    }
            funList.add(func)
        }
    }
}

private class HubData(
        val hubUuid: String,
        val auth: Map<String, String?> = mapOf()
) {
    private val appIdList: HashSet<Map<String, String?>> = hashSetOf()
    private val dataMutex = Mutex()

    fun addAppId(appId: Map<String, String?>) {
        runBlocking {
            dataMutex.withLock {
                appIdList.add(appId)
            }
        }
    }

    fun getAppIdList(): HashSet<Map<String, String?>> = appIdList
}

private fun CoroutinesMutableMap<String, HubData>.getHubData(hubUuid: String, auth: Map<String, String?>): HubData {
    val hubKey = mkHubId(hubUuid, auth)
    return this@getHubData[hubKey] ?: HubData(hubUuid, auth).also {
        this@getHubData[hubKey] = it
    }
}

private fun CoroutinesMutableMap<String, HubData>.popHubData(hubKey: String): HubData? {
    return this@popHubData.remove(hubKey)
}

private fun mkAppId(hubUuid: String, auth: Map<String, String?>, appId: Map<String, String?>) = (hubUuid + auth + appId).md5()

private fun mkHubId(hubUuid: String, auth: Map<String, String?>) = (hubUuid + auth).md5()

private fun mkReleaseRequestBuilder(hubUuid: String, appIdList: Collection<Map<String, String?>>, authMap: Map<String, String?>) =
        ReleaseRequest.newBuilder()
                .setHubUuid(hubUuid)
                .addAllAppIdList(appIdList.map { AppId.newBuilder().addAllAppId(it.togRPCDict()).build() })
                .addAllAuth(authMap.map {
                    Dict.newBuilder().setK(it.key).setV(it.value).build()
                })
