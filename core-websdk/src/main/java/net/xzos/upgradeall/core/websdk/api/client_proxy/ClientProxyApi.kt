package net.xzos.upgradeall.core.websdk.api.client_proxy

import com.google.gson.Gson
import net.xzos.upgradeall.core.utils.data_cache.DataCacheManager
import net.xzos.upgradeall.core.utils.log.Log
import net.xzos.upgradeall.core.utils.log.ObjectTag
import net.xzos.upgradeall.core.utils.log.ObjectTag.Companion.core
import net.xzos.upgradeall.core.utils.log.msg
import net.xzos.upgradeall.core.websdk.api.BaseApi
import net.xzos.upgradeall.core.websdk.api.client_proxy.cloud_config.CloudConfig
import net.xzos.upgradeall.core.websdk.api.client_proxy.hubs.*
import net.xzos.upgradeall.core.websdk.api.web.WebApi
import net.xzos.upgradeall.core.websdk.api.web.proxy.OkhttpProxy
import net.xzos.upgradeall.core.websdk.base_model.AppData
import net.xzos.upgradeall.core.websdk.base_model.MultiRequestData
import net.xzos.upgradeall.core.websdk.base_model.SingleRequestData
import net.xzos.upgradeall.core.websdk.json.CloudConfigList
import net.xzos.upgradeall.core.websdk.json.DownloadItem
import net.xzos.upgradeall.core.websdk.json.ReleaseGson
import net.xzos.upgradeall.getter.GetterPort

internal class ClientProxyApi(dataCache: DataCacheManager) : BaseApi {
    private val okhttpProxy = OkhttpProxy()
    private val cloudConfig = CloudConfig(okhttpProxy)
    private val getterPort = GetterPort()

    private val hubMap: Map<String, BaseHub> = listOf(
//        Github(dataCache, okhttpProxy),
        CoolApk(dataCache, okhttpProxy),
        LsposedRepo(dataCache, okhttpProxy),
        FDroid(dataCache, okhttpProxy),
        Gitlab(dataCache, okhttpProxy),
        GooglePlay(dataCache, okhttpProxy),
    ).associateBy({ it.uuid }, { it })

    override fun getCloudConfig(url: String): CloudConfigList? {
        return try {
            cloudConfig.getCloudConfig(url)
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
                getterPort.checkAppAvailable(
                    data.hub.hubUuid,
                    data.app.appId.map { it.key to (it.value ?: "") }.toMap()
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
                val a = getHub(data.hub.hubUuid).getReleases(data.hub, data.app)
            a
            } catch (e: NoFunction) {
                getterPort.getAppReleases(
                    data.hub.hubUuid,
                    data.app.appId.map { it.key to (it.value ?: "") }.toMap()
                ).run {
                    Gson().fromJson(this, WebApi.releaseListType)
                }
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