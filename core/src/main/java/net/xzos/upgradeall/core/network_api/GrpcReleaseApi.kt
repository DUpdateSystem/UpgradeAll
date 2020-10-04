package net.xzos.upgradeall.core.network_api

import android.os.Looper
import io.grpc.Status
import io.grpc.StatusRuntimeException
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.xzos.upgradeall.core.data.coroutines.CoroutinesMutableList
import net.xzos.upgradeall.core.data.coroutines.CoroutinesMutableMap
import net.xzos.upgradeall.core.data.coroutines.coroutinesMutableListOf
import net.xzos.upgradeall.core.data.coroutines.coroutinesMutableMapOf
import net.xzos.upgradeall.core.route.*
import net.xzos.upgradeall.core.utils.wait
import java.util.concurrent.TimeUnit

class GrpcReleaseApi {
    private val funMap: CoroutinesMutableMap<String, CoroutinesMutableList<(_: List<ReleaseListItem>?) -> Unit>> = coroutinesMutableMapOf(true)
    private val hubDataMap = coroutinesMutableMapOf<String, HubData>(true)
    private val grpcWaitLockList = coroutinesMutableListOf<String>(true)
    private val mutex = Mutex()
    private val grpcRequestLockMap = coroutinesMutableMapOf<String, Mutex>(true)

    suspend fun getAppRelease(hubUuid: String, auth: Map<String, String?>, appId: Map<String, String?>): List<ReleaseListItem>? {
        return if (Looper.myLooper() == Looper.getMainLooper()) {
            null
        } else {
            val itemKey = mkAppId(hubUuid, auth, appId)
            val mutex = grpcRequestLockMap[itemKey] ?: Mutex(true).apply {
                grpcRequestLockMap[itemKey] = this
            }.also {
                GlobalScope.launch {
                    callGetAppReleaseStream(mkHubId(hubUuid, auth))
                }
            }
            var releaseList: List<ReleaseListItem>? = null
            setRequest(hubUuid, auth, appId, fun(list) {
                grpcRequestLockMap.remove(itemKey)?.unlock()
                releaseList = list
            })
            mutex.wait()
            return releaseList
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
        for (appIdL in appIdList.chunked(15))
            GlobalScope.launch {
                callGetAppRelease(hubUuid, auth, appIdL.toHashSet())
            }
    }

    private suspend fun callGetAppRelease(
            hubUuid: String, auth: Map<String, String?>, appIdList: HashSet<Map<String, String?>>
    ) {
        val request = mkReleaseRequestBuilder(hubUuid, appIdList, auth)
        val clearMutex = fun() {
            for (appId in appIdList.toList()) {
                callRequestFun(hubUuid, auth, appId, null)
            }
        }

        val asyncStub = UpdateServerRouteGrpc.newBlockingStub(GrpcApi.mChannel)
        try {
            val releaseResponseIterator = asyncStub.withDeadlineAfter(GrpcApi.deadlineMs, TimeUnit.MILLISECONDS).getAppRelease(request.build())
            var response: ReleaseResponse
            while (withTimeout(GrpcApi.deadlineMs) { releaseResponseIterator.next().also { response = it } } != null) {
                if (response.validHub) {
                    val releasePackage = response.release
                    val appId = releasePackage.appIdList.toMap()
                    val releaseList = if (releasePackage.validData)
                        releasePackage.releaseListList
                    else null
                    callRequestFun(hubUuid, auth, appId, releaseList)
                    appIdList.remove(appId)
                } else {
                    GrpcApi.invalidHubUuidList.add(hubUuid)
                }
            }
        } catch (e: Throwable) {
            if ((e is StatusRuntimeException && e.status.code == Status.Code.DEADLINE_EXCEEDED)
                    || e is TimeoutCancellationException) {
                GrpcApi.logDeadlineError("CallGetAppRelease", request.hubUuid, "hub_uuid: ${hubUuid}, num: ${request.appIdListCount}, cancel: ${appIdList.size}, error:$e")
            }
        } finally {
            clearMutex()
        }
    }

    private fun callRequestFun(hubUuid: String, auth: Map<String, String?>, appId: Map<String, String?>, releaseList: List<ReleaseListItem>?) {
        val itemId = mkAppId(hubUuid, auth, appId)
        funMap.remove(itemId)?.map { func ->
            func(releaseList)
        }
    }

    private suspend fun setRequest(hubUuid: String, auth: Map<String, String?>, appId: Map<String, String?>, func: (_: List<ReleaseListItem>?) -> Unit) {
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

    companion object {
        private const val grpcWaitTime = 200L

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

        private fun mkReleaseRequestBuilder(hubUuid: String, appIdList: HashSet<Map<String, String?>>, authMap: Map<String, String?>) =
                ReleaseRequest.newBuilder()
                        .setHubUuid(hubUuid)
                        .addAllAppIdList(appIdList.map { AppId.newBuilder().addAllAppId(it.togRPCDict()).build() })
                        .addAllAuth(authMap.map {
                            Dict.newBuilder().setK(it.key).setV(it.value).build()
                        })

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
