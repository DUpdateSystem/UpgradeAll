package net.xzos.upgradeall.getter.rpc

import net.xzos.upgradeall.websdk.data.json.CloudConfigList
import net.xzos.upgradeall.websdk.data.json.DownloadItem
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
        globalExpireTime: Long,
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

    suspend fun getCloudConfig(url: String): CloudConfigList

    // ========================================================================
    // Provider Registration
    // ========================================================================

    suspend fun registerProvider(
        hubUuid: String,
        url: String,
    ): Boolean

    // ========================================================================
    // Download Info
    // ========================================================================

    suspend fun getDownloadInfo(
        hubUuid: String,
        appData: Map<String, String>,
        hubData: Map<String, String>,
        assetIndex: List<Int>,
    ): List<DownloadItem>

    // ========================================================================
    // Downloader RPC Methods
    // ========================================================================

    suspend fun downloadSubmit(
        url: String,
        destPath: String,
        headers: Map<String, String>? = null,
        cookies: Map<String, String>? = null,
        hubUuid: String? = null,
    ): TaskIdResponse

    suspend fun downloadGetStatus(taskId: String): TaskInfo

    suspend fun downloadWaitForChange(
        taskId: String,
        timeoutSeconds: Long,
    ): TaskInfo

    suspend fun downloadCancel(taskId: String): Boolean

    suspend fun downloadPause(taskId: String): Boolean

    suspend fun downloadResume(taskId: String): Boolean

    // ========================================================================
    // External Downloader Registration
    // ========================================================================

    suspend fun registerDownloader(
        hubUuid: String,
        rpcUrl: String,
    ): Boolean

    suspend fun unregisterDownloader(hubUuid: String): Boolean

    // ========================================================================
    // App Manager
    // ========================================================================

    suspend fun managerGetApps(): List<AppRecord>

    suspend fun managerSaveApp(record: AppRecord): AppRecord

    suspend fun managerDeleteApp(recordId: String): Boolean

    suspend fun managerGetAppStatus(recordId: String): AppStatus

    suspend fun managerSetVirtualApps(apps: List<AppRecord>): Boolean

    suspend fun managerRenewAll(): Boolean

    // ========================================================================
    // Hub Manager
    // ========================================================================

    suspend fun managerGetHubs(): List<HubRecord>

    suspend fun managerSaveHub(record: HubRecord): Boolean

    suspend fun managerDeleteHub(hubUuid: String): Boolean

    suspend fun managerHubIgnoreApp(
        hubUuid: String,
        appId: Map<String, String?>,
        ignore: Boolean,
    ): Boolean

    suspend fun managerSetApplicationsMode(
        hubUuid: String,
        enable: Boolean,
    ): Boolean

    // ========================================================================
    // Extra Hub
    // ========================================================================

    suspend fun managerGetExtraHubs(): List<ExtraHubRecord>

    suspend fun managerSaveExtraHub(record: ExtraHubRecord): Boolean

    suspend fun managerDeleteExtraHub(id: String): Boolean
}

/**
 * Create GetterService client using WebSocket transport.
 */
fun getClient(url: String): GetterService {
    val wsUrl = url.replace("http://", "ws://").replace("https://", "wss://")
    return GetterServiceImpl(RpcClient(wsUrl))
}
