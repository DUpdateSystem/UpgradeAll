package net.xzos.upgradeall.getter.rpc

import net.xzos.upgradeall.websdk.data.json.CloudConfigList
import net.xzos.upgradeall.websdk.data.json.ReleaseGson

/**
 * Suspend-based GetterService interface for Rust getter RPC.
 * 
 * All methods are suspend functions, enabling efficient async/await patterns
 * over a persistent WebSocket connection.
 */
interface GetterService {
    suspend fun ping(): String

    suspend fun init(
        dataPath: String,
        cachePath: String,
        globalExpireTime: Long
    ): Boolean

    suspend fun shutdown()

    suspend fun checkAppAvailable(
        hubUuid: String,
        appData: Map<String, String>,
        hubData: Map<String, String>,
    ): Boolean

    suspend fun getAppLatestRelease(
        hubUuid: String,
        appData: Map<String, String>,
        hubData: Map<String, String>,
    ): ReleaseGson

    suspend fun getAppReleases(
        hubUuid: String,
        appData: Map<String, String>,
        hubData: Map<String, String>,
    ): List<ReleaseGson>

    suspend fun getCloudConfig(
        url: String
    ): CloudConfigList

    // ========================================================================
    // Downloader RPC Methods
    // ========================================================================

    suspend fun downloadSubmit(
        url: String,
        destPath: String,
        headers: Map<String, String>? = null,
        cookies: Map<String, String>? = null
    ): TaskIdResponse

    suspend fun downloadGetStatus(
        taskId: String
    ): TaskInfo

    suspend fun downloadWaitForChange(
        taskId: String,
        timeoutSeconds: Long
    ): TaskInfo

    suspend fun downloadCancel(
        taskId: String
    ): Boolean

    suspend fun downloadPause(
        taskId: String
    ): Boolean

    suspend fun downloadResume(
        taskId: String
    ): Boolean
}

/**
 * Create GetterService client using WebSocket transport.
 */
fun getClient(url: String): GetterService {
    val wsUrl = url.replace("http://", "ws://").replace("https://", "wss://")
    return GetterServiceImpl(RpcClient(wsUrl))
}
