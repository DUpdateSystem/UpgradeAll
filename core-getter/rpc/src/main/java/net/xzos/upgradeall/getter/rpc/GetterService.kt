package net.xzos.upgradeall.getter.rpc

import com.googlecode.jsonrpc4j.JsonRpcHttpClient
import com.googlecode.jsonrpc4j.JsonRpcMethod
import com.googlecode.jsonrpc4j.JsonRpcParam
import com.googlecode.jsonrpc4j.ProxyUtil
import net.xzos.upgradeall.websdk.data.json.CloudConfigList
import net.xzos.upgradeall.websdk.data.json.ReleaseGson
import java.net.URI

interface GetterService {
    @JsonRpcMethod("ping")
    fun ping(): String

    @JsonRpcMethod("init")
    fun init(
        @JsonRpcParam(value = "data_path") dataPath: String,
        @JsonRpcParam(value = "cache_path") cachePath: String,
        @JsonRpcParam(value = "global_expire_time") globalExpireTime: Long
    ): Boolean

    @JsonRpcMethod("shutdown")
    fun shutdown()

    @JsonRpcMethod("check_app_available")
    fun checkAppAvailable(
        @JsonRpcParam(value = "hub_uuid") hubUuid: String,
        @JsonRpcParam(value = "app_data") appData: Map<String, String>,
        @JsonRpcParam(value = "hub_data") hubData: Map<String, String>,
    ): Boolean

    @JsonRpcMethod("get_latest_release")
    fun getAppLatestRelease(
        @JsonRpcParam(value = "hub_uuid") hubUuid: String,
        @JsonRpcParam(value = "app_data") appData: Map<String, String>,
        @JsonRpcParam(value = "hub_data") hubData: Map<String, String>,
    ): ReleaseGson

    @JsonRpcMethod("get_releases")
    fun getAppReleases(
        @JsonRpcParam(value = "hub_uuid") hubUuid: String,
        @JsonRpcParam(value = "app_data") appData: Map<String, String>,
        @JsonRpcParam(value = "hub_data") hubData: Map<String, String>,
    ): List<ReleaseGson>

    @JsonRpcMethod("get_cloud_config")
    fun getCloudConfig(
        @JsonRpcParam(value = "url") url: String
    ): CloudConfigList

    // ========================================================================
    // Downloader RPC Methods
    // ========================================================================

    @JsonRpcMethod("download_submit")
    fun downloadSubmit(
        @JsonRpcParam(value = "url") url: String,
        @JsonRpcParam(value = "dest_path") destPath: String,
        @JsonRpcParam(value = "headers") headers: Map<String, String>? = null,
        @JsonRpcParam(value = "cookies") cookies: Map<String, String>? = null
    ): TaskIdResponse

    @JsonRpcMethod("download_get_status")
    fun downloadGetStatus(
        @JsonRpcParam(value = "task_id") taskId: String
    ): TaskInfo

    @JsonRpcMethod("download_wait_for_change")
    fun downloadWaitForChange(
        @JsonRpcParam(value = "task_id") taskId: String,
        @JsonRpcParam(value = "timeout_seconds") timeoutSeconds: Long
    ): TaskInfo

    @JsonRpcMethod("download_cancel")
    fun downloadCancel(
        @JsonRpcParam(value = "task_id") taskId: String
    ): Boolean

    @JsonRpcMethod("download_pause")
    fun downloadPause(
        @JsonRpcParam(value = "task_id") taskId: String
    ): Boolean

    @JsonRpcMethod("download_resume")
    fun downloadResume(
        @JsonRpcParam(value = "task_id") taskId: String
    ): Boolean
}

/**
 * Create HTTP-based GetterService client (for backward compatibility and testing)
 */
fun getHttpClient(url: String): GetterService {
    val client = JsonRpcHttpClient(URI(url).toURL())

    return ProxyUtil.createClientProxy(
        client.javaClass.classLoader,
        GetterService::class.java,
        client
    )
}

/**
 * Create WebSocket-based SuspendGetterService client (primary implementation)
 */
fun getWsClient(url: String): SuspendGetterService {
    val wsUrl = url.replace("http://", "ws://").replace("https://", "wss://")
    return WsGetterService(WsRpcClient(wsUrl))
}

/**
 * Default client factory - uses WebSocket for better performance
 */
fun getClient(url: String): GetterService = getHttpClient(url)

