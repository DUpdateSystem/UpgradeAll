package net.xzos.upgradeall.core.network_api

import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.StatusRuntimeException
import net.xzos.upgradeall.core.data.config.AppConfig
import net.xzos.upgradeall.core.data_manager.utils.DataCache
import net.xzos.upgradeall.core.route.AppInfo
import net.xzos.upgradeall.core.route.AppInfoItem
import net.xzos.upgradeall.core.route.ReleaseInfoItem
import net.xzos.upgradeall.core.route.UpdateServerRouteGrpc


object GrpcApi {

    private val invalidHubUuidList: MutableList<String> = mutableListOf()
    private var mChannel: ManagedChannel = ManagedChannelBuilder.forTarget(AppConfig.update_server_url).usePlaintext().build()

    init {
        renew()
    }

    fun renew() {
        mChannel = ManagedChannelBuilder.forTarget(AppConfig.update_server_url).usePlaintext().build()
    }

    fun getReleaseInfo(hubUuid: String, appInfo: List<AppInfoItem>): Pair<List<ReleaseInfoItem>?, Boolean> {
        if (hubUuid in invalidHubUuidList) return Pair(null, true)
        if (DataCache.existsCache(hubUuid, appInfo))
            return Pair(DataCache.getReleaseInfo(hubUuid, appInfo), true)
        val blockingStub = UpdateServerRouteGrpc.newBlockingStub(mChannel)
        val request = AppInfo.newBuilder().setHubUuid(hubUuid).apply {
            for (infoItem in appInfo) {
                addAppInfo(AppInfoItem.newBuilder().setKey(infoItem.key).setValue(infoItem.value).build())
            }
        }.build()
        val returnValue = try {
            blockingStub.getReleaseInfo(request)
        } catch (ignore: StatusRuntimeException) {
            return Pair(null, true)
        }
        return if (!returnValue.validHubUuid) {
            invalidHubUuidList.add(hubUuid)
            Pair(null, true)
        } else {
            val releaseInfoList = returnValue.releaseInfoList
            DataCache.cacheReleaseInfo(hubUuid, appInfo, releaseInfoList)
            if (!returnValue.validApp) {
                Pair(null, false)
            } else {
                Pair(releaseInfoList, true)
            }
        }
    }
}