package net.xzos.upgradeall.core.module.network

import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.xzos.upgradeall.core.log.Log
import net.xzos.upgradeall.core.log.msg
import net.xzos.upgradeall.core.route.*
import net.xzos.upgradeall.core.utils.chunked
import net.xzos.upgradeall.core.utils.coroutines.*
import net.xzos.upgradeall.core.utils.md5
import net.xzos.upgradeall.core.utils.watchdog.WatchdogItem
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

object GrpcReleaseApi {
    private const val grpcWaitTime = 500L

    private val hubDataMap = coroutinesMutableMapOf<String, HubData>(true)
    private val renewLock = Mutex()

    fun getAppRelease(
        hubUuid: String, auth: Map<String, String?>, appId: Map<String, String?>, priority: Int,
        callback: (List<ReleaseListItem>?) -> Unit
    ) {
        setRequest(hubUuid, auth, appId, priority, callback)
        GlobalScope.launch(Dispatchers.IO) {
            renewCallGetAppReleaseStream()
        }
    }

    private suspend fun renewCallGetAppReleaseStream() {
        if (!renewLock.isLocked) {
            renewLock.withLock {
                while (hubDataMap.isNotEmpty()) {
                    delay(grpcWaitTime)
                    hubDataMap.forEach {
                        hubDataMap.remove(it.key)
                        initCallGetAppReleaseStream(it.value)
                    }
                }
            }
        }
    }

    private fun initCallGetAppReleaseStream(hubData: HubData) {
        val hubUuid = hubData.hubUuid
        val auth = hubData.auth
        val appItemMap = hubData.getAppItemMap()
        android.util.Log.e(
            "update record",
            "initSend: hub_uuid: $hubUuid, size: ${appItemMap.size}"
        )
        chunkedCallGetAppRelease(hubUuid, auth, appItemMap, appItemMap.size)
    }

    private fun chunkedCallGetAppRelease(
        hubUuid: String, auth: Map<String, String?>,
        appItemMap: MutableMap<Map<String, String?>, CoroutinesMutableList<(List<ReleaseListItem>?) -> Unit>>,
        chunkedSize: Int = if (appItemMap.size > 10) appItemMap.size / 2 else appItemMap.size,
        autoRetryNum: Int = 3,
    ) {
        if (appItemMap.isNotEmpty())
            appItemMap.chunked(chunkedSize).forEach { chunkedMap ->
                GlobalScope.launch(Dispatchers.IO) {
                    callGetAppRelease(hubUuid, auth, chunkedMap, autoRetryNum)
                }
            }
    }

    private fun callGetAppRelease(
        hubUuid: String, auth: Map<String, String?>,
        appItemMap: MutableMap<Map<String, String?>, CoroutinesMutableList<(List<ReleaseListItem>?) -> Unit>>,
        autoRetryNum: Int,
    ) {
        val stub = GrpcApi.getStub()
            ?.withDeadlineAfter(GrpcApi.getDeadlineMs(appItemMap.size), TimeUnit.MILLISECONDS)
            ?: return
        val request = mkReleaseRequestBuilder(hubUuid, appItemMap.keys, auth)
        val finishFun = { callFinishCheck(hubUuid, auth, appItemMap, autoRetryNum) }
        getNextResponse(stub, request.build(), { i, response ->
            if (i == 0 && !response.validHub) {
                GrpcApi.invalidHubUuidList.add(hubUuid)
                callFailCheck(hubUuid, appItemMap)
                appItemMap.clear()
            } else {
                val releasePackage = response.release
                val appId = releasePackage.appIdList.toMap()
                val releaseList = if (releasePackage.validData)
                    releasePackage.releaseListList
                else null
                appItemMap.remove(appId)?.forEach { it(releaseList) }
            }
        }, { e ->
            Log.w(
                GrpcApi.logObjectTag, GrpcApi.TAG,
                "callGetAppRelease: Error record e: ${e?.msg()}".trimIndent()
            )
            if ((e is StatusRuntimeException && e.status.code == Status.Code.DEADLINE_EXCEEDED)
                || e is TimeoutException
            ) {
                GrpcApi.logDeadlineError(
                    "CallGetAppRelease", request.hubUuid,
                    "hub_uuid: ${hubUuid}, num: ${request.appIdListCount}, cancel: ${appItemMap.size}, error:$e"
                )
                GrpcApi.removeStub(stub)
            } else {
                Log.w(
                    GrpcApi.logObjectTag, GrpcApi.TAG,
                    "callGetAppRelease: 请求失败 hub_uuid: $hubUuid e: ${e?.msg()}".trimIndent()
                )
            }
            finishFun()
        }, finishFun)
    }

    private fun getNextResponse(
        stub: UpdateServerRouteGrpc.UpdateServerRouteStub, request: ReleaseRequest,
        nextCallback: (Int, ReleaseResponse) -> Unit,
        failCallback: (t: Throwable?) -> Unit, completedCallback: () -> Unit,
    ) {
        val watchdog = WatchdogItem(GrpcApi.deadlineMs).apply {
            addStopListener {
                failCallback(TimeoutException())
            }
        }
        val streamObserver = object : StreamObserver<ReleaseResponse> {
            var index = 0
            override fun onNext(value: ReleaseResponse?) {
                watchdog.ping()
                value?.let {
                    nextCallback(index, it)
                }
                index += 1
            }

            override fun onError(t: Throwable?) {
                watchdog.stop()
                failCallback(t)
            }

            override fun onCompleted() {
                completedCallback()
                watchdog.stop()
            }
        }
        GlobalScope.launch(Dispatchers.IO) {
            watchdog.start()
        }
        stub.getAppRelease(request, streamObserver)
    }

    private fun callFinishCheck(
        hubUuid: String, auth: Map<String, String?>,
        appItemMap: MutableMap<Map<String, String?>, CoroutinesMutableList<(List<ReleaseListItem>?) -> Unit>>,
        autoRetryNum: Int
    ) {
        val size = appItemMap.size
        if (size > 0) {
            if (autoRetryNum > 0) {
                Log.w(
                    GrpcApi.logObjectTag, GrpcApi.TAG,
                    "callGetAppRelease: 重新请求 hub_uuid: $hubUuid num: $size"
                )
                chunkedCallGetAppRelease(
                    hubUuid, auth, appItemMap, autoRetryNum = autoRetryNum - 1
                )
            } else {
                callFailCheck(hubUuid, appItemMap)
            }
        }
    }

    private fun callFailCheck(
        hubUuid: String,
        appItemMap: Map<Map<String, String?>, MutableList<(List<ReleaseListItem>?) -> Unit>>,
    ) {
        val size = appItemMap.size
        if (size > 0) {
            Log.w(
                GrpcApi.logObjectTag, GrpcApi.TAG,
                "callGetAppRelease: 放弃请求 hub_uuid: $hubUuid num: $size"
            )
            appItemMap.forEach { it.value.forEach { it(null) } }  // call callback function with fail value
        }
    }

    private fun setRequest(
        hubUuid: String, auth: Map<String, String?>, appId: Map<String, String?>, priority: Int,
        callback: (List<ReleaseListItem>?) -> Unit,
    ) {
        hubDataMap.getHubData(hubUuid, auth).addAppItem(appId, priority, callback).let {
            if (!it) callback(null)
        }
    }
}

private class HubData(
    val hubUuid: String,
    val auth: Map<String, String?> = mapOf()
) {
    private val appItemMap = coroutinesMutableMapOf<
            Int, CoroutinesMutableMap<Map<String, String?>,
            CoroutinesMutableList<(List<ReleaseListItem>?) -> Unit>>>(true)

    fun addAppItem(
        appId: Map<String, String?>, priority: Int, callback: (List<ReleaseListItem>?) -> Unit
    ): Boolean {
        val list = appItemMap.getOrDefault(priority, coroutinesMutableMapOf(true))
            .getOrDefault(appId, coroutinesMutableListOf(true))
        return list.add(callback)
    }

    fun getAppItemMap(): MutableMap<Map<String, String?>, CoroutinesMutableList<(List<ReleaseListItem>?) -> Unit>> {
        val map = appItemMap
            .filter { it.value.isNotEmpty() }
            .toSortedMap(compareByDescending { it })
        return if (map.isNotEmpty())
            mutableMapOf<Map<String, String?>,
                    CoroutinesMutableList<(List<ReleaseListItem>?) -> Unit>>().apply {
                map.forEach { putAll(it.value) }
            }
        else mutableMapOf()
    }
}

private fun CoroutinesMutableMap<String, HubData>.getHubData(
    hubUuid: String,
    auth: Map<String, String?>
): HubData {
    val hubKey = mkHubId(hubUuid, auth)
    return this.getOrDefault(hubKey, HubData(hubUuid, auth))
}

private fun CoroutinesMutableMap<String, HubData>.popHubData(hubKey: String): HubData? {
    return this@popHubData.remove(hubKey)
}

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