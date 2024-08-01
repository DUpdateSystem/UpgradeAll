package net.xzos.upgradeall.core.websdk.api.client_proxy

import net.xzos.upgradeall.core.utils.data_cache.DataCacheManager
import net.xzos.upgradeall.core.utils.log.Log
import net.xzos.upgradeall.core.utils.log.ObjectTag
import net.xzos.upgradeall.core.utils.log.ObjectTag.Companion.core
import net.xzos.upgradeall.core.utils.log.msg
import net.xzos.upgradeall.core.websdk.api.BaseApi
import net.xzos.upgradeall.core.websdk.api.client_proxy.hubs.BaseHub
import net.xzos.upgradeall.core.websdk.api.client_proxy.hubs.CoolApk
import net.xzos.upgradeall.core.websdk.api.client_proxy.hubs.GooglePlay
import net.xzos.upgradeall.core.websdk.api.web.proxy.OkhttpProxy
import net.xzos.upgradeall.core.websdk.base_model.AppData
import net.xzos.upgradeall.core.websdk.base_model.MultiRequestData
import net.xzos.upgradeall.core.websdk.base_model.SingleRequestData
import net.xzos.upgradeall.core.websdk.getterPort
import net.xzos.upgradeall.websdk.data.json.CloudConfigList
import net.xzos.upgradeall.websdk.data.json.DownloadItem
import net.xzos.upgradeall.websdk.data.json.ReleaseGson

internal class ClientProxyApi(dataCache: DataCacheManager) : BaseApi {
    private val okhttpProxy = OkhttpProxy()

    private val hubMap: Map<String, BaseHub> = listOf(
        CoolApk(dataCache, okhttpProxy),
        GooglePlay(dataCache, okhttpProxy),
    ).associateBy({ it.uuid }, { it })

    override fun getCloudConfig(url: String): CloudConfigList? {
        return try {
            getterPort.getCloudConfig(url)
        } catch (e: Throwable) {
            Log.e(logObjectTag, TAG, e.stackTraceToString())
            null
        }
    }

    override fun checkAppAvailable(data: SingleRequestData): Boolean? {
        return runFun("checkAppAvailable") {
            try {
                getHub(data.hub.hubUuid).checkAppAvailable(data.hub, data.app)
            } catch (e: NoFunction) {
                val hubMap = data.hub.auth + data.hub.other
                getterPort.checkAppAvailable(
                    data.hub.hubUuid,
                    data.app.appId.map { it.key to (it.value ?: "") }.toMap(),
                    hubMap.map { it.key to (it.value ?: "") }.toMap(),
                )
            }
        }
    }

    override fun getAppUpdate(data: MultiRequestData): Map<AppData, ReleaseGson?>? {
        return runFun("getAppUpdate") {
            try {
                getHub(data.hub.hubUuid).getUpdate(data.hub, data.appList)
            } catch (e: NoFunction) {
                data.appList.associateWith {
                    getAppReleaseList(
                        SingleRequestData(
                            data.hub,
                            it
                        )
                    )?.first()
                }
            }
        }
    }

    override fun getAppReleaseList(data: SingleRequestData): List<ReleaseGson>? {
        return runFun("getAppReleaseList") {
            try {
                getHub(data.hub.hubUuid).getReleases(data.hub, data.app)
            } catch (e: NoFunction) {
                val hubMap = data.hub.auth + data.hub.other
                getterPort.getAppReleases(
                    data.hub.hubUuid,
                    data.app.appId.map { it.key to (it.value ?: "") }.toMap(),
                    hubMap.map { it.key to (it.value ?: "") }.toMap(),
                )
            }
        }
    }

    override fun getDownloadInfo(
        data: SingleRequestData,
        assetIndex: Pair<Int, Int>
    ): List<DownloadItem>? {
        val assets = getAppReleaseList(data)
            ?.get(assetIndex.first)?.assetGsonList?.get(assetIndex.second)
        return runFun("getDownloadInfo") {
            getHub(data.hub.hubUuid).getDownload(data.hub, data.app, assetIndex.toList(), assets)
        }
    }

    private fun getHub(uuid: String): BaseHub {
        return hubMap[uuid] ?: throw NoFunction()
    }

    private fun <O> runFun(funcName: String, func: () -> O): O? {
        return try {
            func()
        } catch (e: Throwable) {
            Log.e(logObjectTag, TAG, "$funcName: ${e.msg()}")
            null
        }
    }

    companion object {
        private const val TAG = "ClientProxyApi"
        private val logObjectTag = ObjectTag(core, TAG)
    }
}

class NoFunction : RuntimeException()