package net.xzos.upgradeall.core.websdk.api.client_proxy

import net.xzos.upgradeall.core.utils.data_cache.DataCacheManager
import net.xzos.upgradeall.core.utils.log.Log
import net.xzos.upgradeall.core.utils.log.ObjectTag
import net.xzos.upgradeall.core.utils.log.ObjectTag.Companion.core
import net.xzos.upgradeall.core.websdk.api.BaseApi
import net.xzos.upgradeall.core.websdk.api.client_proxy.cloud_config.CloudConfig
import net.xzos.upgradeall.core.websdk.api.client_proxy.hubs.*
import net.xzos.upgradeall.core.websdk.api.web.proxy.OkhttpProxy
import net.xzos.upgradeall.core.websdk.base_model.ApiRequestData
import net.xzos.upgradeall.core.websdk.json.CloudConfigList
import net.xzos.upgradeall.core.websdk.json.DownloadItem
import net.xzos.upgradeall.core.websdk.json.ReleaseGson

internal class ClientProxyApi(dataCache: DataCacheManager) : BaseApi {
    private val okhttpProxy = OkhttpProxy()
    private val cloudConfig = CloudConfig(okhttpProxy)

    private val hubMap: Map<String, BaseHub> = listOf(
        Github(dataCache, okhttpProxy),
        CoolApk(dataCache, okhttpProxy),
        LsposedRepo(dataCache, okhttpProxy),
        FDroid(dataCache, okhttpProxy),
        Gitlab(dataCache, okhttpProxy),
    ).associateBy({ it.uuid }, { it })

    override fun getCloudConfig(url: String): CloudConfigList? {
        return try {
            cloudConfig.getCloudConfig(url)
        } catch (e: Throwable) {
            Log.e(logObjectTag, TAG, e.stackTraceToString())
            null
        }
    }

    private fun getHub(uuid: String): BaseHub {
        return hubMap[uuid] ?: throw NoFunction()
    }

    override fun getAppRelease(data: ApiRequestData): List<ReleaseGson>? {
        val hub = getHub(data.hubUuid)
        return try {
            hub.getRelease(data)
        } catch (e: Throwable) {
            Log.e(logObjectTag, TAG, e.stackTraceToString())
            null
        }
    }

    override fun getAppReleaseList(data: ApiRequestData): List<ReleaseGson>? {
        val hub = getHub(data.hubUuid)
        return try {
            hub.getRelease(data)
        } catch (e: Throwable) {
            Log.e(logObjectTag, TAG, e.stackTraceToString())
            null
        }
    }

    override fun getDownloadInfo(
        data: ApiRequestData,
        assetIndex: Pair<Int, Int>
    ): List<DownloadItem> {
        val hub = getHub(data.hubUuid)
        val assets = getAppReleaseList(data)
            ?.get(assetIndex.first)?.assetGsonList?.get(assetIndex.second)
        return try {
            hub.getDownload(data, assetIndex.toList(), assets)
        } catch (e: Throwable) {
            Log.e(logObjectTag, TAG, e.stackTraceToString())
            null
        } ?: emptyList()
    }

    companion object {
        private const val TAG = "ClientProxyApi"
        private val logObjectTag = ObjectTag(core, TAG)
    }
}

class NoFunction : RuntimeException()