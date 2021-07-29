package net.xzos.upgradeall.core.module.network

import io.grpc.ManagedChannel
import io.grpc.Status
import io.grpc.StatusRuntimeException
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.xzos.upgradeall.core.log.Log
import net.xzos.upgradeall.core.log.msg
import net.xzos.upgradeall.core.route.*
import net.xzos.upgradeall.core.utils.coroutines.CoroutinesMutableMap
import net.xzos.upgradeall.core.utils.coroutines.coroutinesMutableListOf
import net.xzos.upgradeall.core.utils.coroutines.coroutinesMutableMapOf
import net.xzos.upgradeall.core.utils.coroutines.toCoroutinesMutableList
import net.xzos.upgradeall.core.utils.md5
import net.xzos.upgradeall.core.utils.watchdog.WatchdogItem

internal object GrpcReleaseApi {
    private const val grpcWaitTime = 50L
    private var chunkedSize = 200

    private val callbackFunMap: CoroutinesMutableMap<String, (_: List<ReleaseListItem>?) -> Unit> =
        coroutinesMutableMapOf(true)
    private val hubDataMap = coroutinesMutableMapOf<String, HubData>(true)
    private val grpcWaitLockList = coroutinesMutableListOf<String>(true)
    private val mutex = Mutex()
    private val grpcRequestLockMap = coroutinesMutableMapOf<String, WatchdogItem>(true)

    suspend fun getAppRelease(
        hubUuid: String,
        auth: Map<String, String?>,
        appId: Map<String, String?>,
        priority: Int
    ): List<ReleaseListItem>? {
        var releaseList: List<ReleaseListItem>? = null
        val watchdog = setRequest(hubUuid, auth, appId, fun(list) {
            releaseList = list
        }, priority)
        watchdog.start()
        GlobalScope.launch(Dispatchers.IO) {
            callGetAppReleaseStream(mkHubId(hubUuid, auth))
        }
        watchdog.block()
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
        appIdList.forEach {
            val itemId = mkAppId(hubUuid, auth, it)
            grpcRequestLockMap[itemId]?.ping()
        }
        val channel = GrpcApi.getChannel()
        appIdList.chunked(chunkedSize).forEach { chunkedList ->
            GlobalScope.launch {
                callGetAppRelease(hubUuid, auth, chunkedList, autoRetryNum, channel)
            }
        }
    }

    private suspend fun callGetAppRelease(
        hubUuid: String, auth: Map<String, String?>,
        appIdList0: Collection<Map<String, String?>>, autoRetryNum: Int,
        channel: ManagedChannel?,
    ) {
        val appIdList = appIdList0.toCoroutinesMutableList(true)
        val request = mkReleaseRequestBuilder(hubUuid, appIdList, auth)
        val clearMutex = fun() {
            val size = appIdList.size
            if (size > 0) {
                Log.w(
                    GrpcApi.logObjectTag, GrpcApi.TAG,
                    "callGetAppRelease: 放弃请求 hub_uuid: $hubUuid num: $size"
                )
                for (appId in appIdList.toList()) {
                    runCallbackFun(hubUuid, auth, appId, null)
                }
            }
        }
        var pingNum = 5
        val pingWatchdog = fun() {
            pingNum = (pingNum + 1) % 5
            if (pingNum == 0)
                appIdList.forEach {
                    val itemId = mkAppId(hubUuid, auth, it)
                    grpcRequestLockMap[itemId]?.ping()
                }
        }
        if (hubUuid in GrpcApi.invalidHubUuidList || channel == null) {
            clearMutex()
            return
        }
        try {
            val blockingStub = UpdateServerRouteGrpc.newBlockingStub(channel)
            val releaseResponseIterator = blockingStub.getAppRelease(request.build())
            var firstIndex = true
            while (withTimeout(GrpcApi.deadlineMs) {
                    releaseResponseIterator.hasNext()
                }
            ) {
                pingWatchdog()
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
                runCallbackFun(hubUuid, auth, appId, releaseList)
                appIdList.remove(appId)
            }
        } catch (e: Throwable) {
            if ((e is StatusRuntimeException && e.status.code == Status.Code.DEADLINE_EXCEEDED)
                || e is TimeoutCancellationException
            ) {
                GrpcApi.logDeadlineError(
                    "CallGetAppRelease", request.hubUuid,
                    "hub_uuid: ${hubUuid}, num: ${request.appIdListCount}, cancel: ${appIdList.size}, error:$e"
                )
                if (chunkedSize > 25) {
                    chunkedSize = (chunkedSize / 1.2).toInt()
                }
            } else {
                Log.w(
                    GrpcApi.logObjectTag,
                    GrpcApi.TAG,
                    "callGetAppRelease: 请求失败 hub_uuid: $hubUuid e: ${e.msg()}".trimIndent()
                )
            }
        } finally {
            val size = appIdList.size
            if (size > 0) {
                if (autoRetryNum > 0) {
                    Log.w(
                        GrpcApi.logObjectTag,
                        GrpcApi.TAG,
                        "callGetAppRelease: 重新请求 hub_uuid: $hubUuid num: $size"
                    )
                    chunkedCallGetAppRelease(hubUuid, auth, appIdList, autoRetryNum - 1)
                } else {
                    clearMutex()
                }
            }
        }
    }

    private fun runCallbackFun(
        hubUuid: String,
        auth: Map<String, String?>,
        appId: Map<String, String?>,
        releaseList: List<ReleaseListItem>?
    ) {
        val itemId = mkAppId(hubUuid, auth, appId)
        callbackFunMap.remove(itemId)?.let { func ->
            releaseList?.run { func(this) }
        }
        grpcRequestLockMap.remove(itemId)?.stop()
    }

    private suspend fun setRequest(
        hubUuid: String,
        auth: Map<String, String?>,
        appId: Map<String, String?>,
        func: (_: List<ReleaseListItem>?) -> Unit,
        priority: Int
    ): WatchdogItem {
        mutex.withLock {
            val itemKey = mkAppId(hubUuid, auth, appId)
            val watchdog = grpcRequestLockMap[itemKey] ?: WatchdogItem(GrpcApi.deadlineMs).apply {
                grpcRequestLockMap[itemKey] = this
                this.addStopListener {
                    runCallbackFun(hubUuid, auth, appId, null)
                }
            }
            hubDataMap.getHubData(hubUuid, auth).addAppId(appId, priority)
            callbackFunMap[itemKey] = func
            return watchdog
        }
    }
}

private class HubData(
    val hubUuid: String,
    val auth: Map<String, String?> = mapOf()
) {
    private val appIdMap: CoroutinesMutableMap<Int, HashSet<Map<String, String?>>> =
        coroutinesMutableMapOf(true)
    private val dataMutex = Mutex()

    suspend fun addAppId(appId: Map<String, String?>, priority: Int) {
        dataMutex.withLock {
            val list = appIdMap.get(priority, hashSetOf())
            list.add(appId)
        }
    }

    suspend fun getAppIdList(): HashSet<Map<String, String?>> {
        dataMutex.withLock {
            val map = appIdMap
                .filter { it.value.isNotEmpty() }
                .toSortedMap(compareByDescending { it })
            return if (map.isNotEmpty()) {
                val key = map.firstKey()
                appIdMap.remove(key)
                map[key]!!
            } else hashSetOf()
        }
    }
}

private fun CoroutinesMutableMap<String, HubData>.getHubData(
    hubUuid: String,
    auth: Map<String, String?>
): HubData {
    val hubKey = mkHubId(hubUuid, auth)
    return this@getHubData[hubKey] ?: HubData(hubUuid, auth).also {
        this@getHubData[hubKey] = it
    }
}

private fun CoroutinesMutableMap<String, HubData>.popHubData(hubKey: String): HubData? {
    return this@popHubData.remove(hubKey)
}

private fun mkAppId(hubUuid: String, auth: Map<String, String?>, appId: Map<String, String?>) =
    (hubUuid + auth + appId).md5()

private fun mkHubId(hubUuid: String, auth: Map<String, String?>) = (hubUuid + auth).md5()

private fun mkReleaseRequestBuilder(
    hubUuid: String,
    appIdList: Collection<Map<String, String?>>,
    authMap: Map<String, String?>
) = ReleaseRequest.newBuilder()
    .setHubUuid(hubUuid)
    .addAllAppIdList(appIdList.map { AppId.newBuilder().addAllAppId(it.togRPCDict()).build() })
    .addAllAuth(authMap.map {
        Dict.newBuilder().setK(it.key).setV(it.value).build()
    })