package net.xzos.upgradeall.core.network_api

import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.StatusRuntimeException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import net.xzos.upgradeall.core.data.config.AppConfig
import net.xzos.upgradeall.core.data.json.nongson.ObjectTag
import net.xzos.upgradeall.core.data_manager.utils.DataCache
import net.xzos.upgradeall.core.log.Log
import net.xzos.upgradeall.core.route.AppInfo
import net.xzos.upgradeall.core.route.AppInfoItem
import net.xzos.upgradeall.core.route.ReturnValue
import net.xzos.upgradeall.core.route.UpdateServerRouteGrpc


object GrpcApi {

    private const val TAG = "GrpcApi"
    private val logObjectTag = ObjectTag(ObjectTag.core, TAG)
    private val invalidHubUuidList: MutableList<String> = mutableListOf()
    private var mChannel: ManagedChannel = ManagedChannelBuilder.forTarget(AppConfig.update_server_url).usePlaintext().build()

    init {
        renew()
    }

    fun renew() {
        mChannel = ManagedChannelBuilder.forTarget(AppConfig.update_server_url).usePlaintext().build()
    }

    suspend fun getReturnValue(hubUuid: String, appInfo: List<AppInfoItem>): ReturnValue? {
        if (hubUuid in invalidHubUuidList) return null
        if (DataCache.existsCache(hubUuid, appInfo))
            return DataCache.getReleaseInfo(hubUuid, appInfo)
        val blockingStub = UpdateServerRouteGrpc.newBlockingStub(mChannel)
        val request = AppInfo.newBuilder().setHubUuid(hubUuid).apply {
            for (infoItem in appInfo) {
                addAppInfo(AppInfoItem.newBuilder().setKey(infoItem.key).setValue(infoItem.value).build())
            }
        }.build()
        val returnValue = try {
            withTimeout(15000L) {
                blockingStub.getReleaseInfo(request)
            }
        } catch (e: TimeoutCancellationException) {
            Log.w(logObjectTag, TAG, """请求超时，取消
                hub_uuid: $hubUuid
                app_info: $appInfo
            """.trimIndent())
            return null
        } catch (ignore: StatusRuntimeException) {
            return null
        }
        return if (!returnValue.validHubUuid) {
            invalidHubUuidList.add(hubUuid)
            null
        } else {
            DataCache.cacheReleaseInfo(hubUuid, appInfo, returnValue)
            returnValue
        }
    }
}