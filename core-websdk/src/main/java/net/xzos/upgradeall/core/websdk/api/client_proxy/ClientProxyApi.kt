package net.xzos.upgradeall.core.websdk.api.client_proxy

import net.xzos.upgradeall.core.utils.log.Log
import net.xzos.upgradeall.core.utils.log.ObjectTag
import net.xzos.upgradeall.core.utils.log.ObjectTag.Companion.core
import net.xzos.upgradeall.core.utils.log.msg
import net.xzos.upgradeall.core.websdk.api.BaseApi
import net.xzos.upgradeall.core.websdk.base_model.AppData
import net.xzos.upgradeall.core.websdk.base_model.MultiRequestData
import net.xzos.upgradeall.core.websdk.base_model.SingleRequestData
import net.xzos.upgradeall.core.websdk.getterPort
import net.xzos.upgradeall.websdk.data.json.CloudConfigList
import net.xzos.upgradeall.websdk.data.json.DownloadItem
import net.xzos.upgradeall.websdk.data.json.ReleaseGson

/**
 * Client proxy API that delegates ALL requests to the Rust getter via JSON-RPC.
 *
 * Kotlin-based hubs (GooglePlay, CoolApk) are registered as OutsideProviders in the Rust getter
 * at startup, so the getter handles routing to them via HTTP JSON-RPC callbacks.
 * This eliminates the need for hub-specific routing logic or fallback mechanisms here.
 */
internal class ClientProxyApi : BaseApi {
    override fun getCloudConfig(url: String): CloudConfigList? =
        runFun("getCloudConfig") {
            getterPort.getCloudConfig(url)
        }

    override fun checkAppAvailable(data: SingleRequestData): Boolean? =
        runFun("checkAppAvailable") {
            val hubMap = data.hub.auth + data.hub.other
            getterPort.checkAppAvailable(
                data.hub.hubUuid,
                data.app.appId.mapValues { it.value ?: "" },
                hubMap.mapValues { it.value ?: "" },
            )
        }

    override fun getAppUpdate(data: MultiRequestData): Map<AppData, ReleaseGson?>? =
        runFun("getAppUpdate") {
            data.appList.associateWith {
                getAppReleaseList(SingleRequestData(data.hub, it))?.firstOrNull()
            }
        }

    override fun getAppReleaseList(data: SingleRequestData): List<ReleaseGson>? =
        runFun("getAppReleaseList") {
            val hubMap = data.hub.auth + data.hub.other
            getterPort.getAppReleases(
                data.hub.hubUuid,
                data.app.appId.mapValues { it.value ?: "" },
                hubMap.mapValues { it.value ?: "" },
            )
        }

    override fun getDownloadInfo(
        data: SingleRequestData,
        assetIndex: Pair<Int, Int>,
    ): List<DownloadItem>? =
        runFun("getDownloadInfo") {
            val hubMap = data.hub.auth + data.hub.other
            getterPort.getDownloadInfo(
                data.hub.hubUuid,
                data.app.appId.mapValues { it.value ?: "" },
                hubMap.mapValues { it.value ?: "" },
                listOf(assetIndex.first, assetIndex.second),
            )
        }

    private fun <O> runFun(
        funcName: String,
        func: () -> O,
    ): O? =
        try {
            func()
        } catch (e: Throwable) {
            Log.e(logObjectTag, TAG, "$funcName: ${e.msg()}")
            null
        }

    companion object {
        private const val TAG = "ClientProxyApi"
        private val logObjectTag = ObjectTag(core, TAG)
    }
}
