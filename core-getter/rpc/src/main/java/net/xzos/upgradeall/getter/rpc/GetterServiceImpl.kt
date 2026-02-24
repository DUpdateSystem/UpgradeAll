package net.xzos.upgradeall.getter.rpc

import net.xzos.upgradeall.websdk.data.json.CloudConfigList
import net.xzos.upgradeall.websdk.data.json.DownloadItem
import net.xzos.upgradeall.websdk.data.json.ReleaseGson

/**
 * WebSocket-based implementation of GetterService.
 *
 * Uses RpcClient internally to communicate with the Rust getter server
 * over a persistent WebSocket connection.
 */
class GetterServiceImpl(
    private val client: RpcClient,
) : GetterService {
    override suspend fun ping(): String = client.invoke("ping", typeOf<String>())

    override suspend fun init(
        dataPath: String,
        cachePath: String,
        globalExpireTime: Long,
    ): Boolean {
        val params =
            mapOf(
                "data_path" to dataPath,
                "cache_path" to cachePath,
                "global_expire_time" to globalExpireTime,
            )
        return client.invoke("init", params, typeOf<Boolean>())
    }

    override suspend fun shutdown() {
        client.invokeVoid("shutdown")
    }

    override suspend fun checkAppAvailable(
        hubUuid: String,
        appData: Map<String, String>,
        hubData: Map<String, String>,
    ): Boolean {
        val params =
            mapOf(
                "hub_uuid" to hubUuid,
                "app_data" to appData,
                "hub_data" to hubData,
            )
        return client.invoke("check_app_available", params, typeOf<Boolean>())
    }

    override suspend fun getAppLatestRelease(
        hubUuid: String,
        appData: Map<String, String>,
        hubData: Map<String, String>,
    ): ReleaseGson {
        val params =
            mapOf(
                "hub_uuid" to hubUuid,
                "app_data" to appData,
                "hub_data" to hubData,
            )
        return client.invoke("get_latest_release", params, typeOf<ReleaseGson>())
    }

    override suspend fun getAppReleases(
        hubUuid: String,
        appData: Map<String, String>,
        hubData: Map<String, String>,
    ): List<ReleaseGson> {
        val params =
            mapOf(
                "hub_uuid" to hubUuid,
                "app_data" to appData,
                "hub_data" to hubData,
            )
        return client.invoke("get_releases", params, typeOf<List<ReleaseGson>>())
    }

    override suspend fun getCloudConfig(url: String): CloudConfigList {
        val params = mapOf("api_url" to url)
        return client.invoke("get_cloud_config", params, typeOf<CloudConfigList>())
    }

    // ========================================================================
    // Provider Registration
    // ========================================================================

    override suspend fun registerProvider(
        hubUuid: String,
        url: String,
    ): Boolean {
        val params =
            mapOf(
                "hub_uuid" to hubUuid,
                "url" to url,
            )
        return client.invoke("register_provider", params, typeOf<Boolean>())
    }

    // ========================================================================
    // Download Info
    // ========================================================================

    override suspend fun getDownloadInfo(
        hubUuid: String,
        appData: Map<String, String>,
        hubData: Map<String, String>,
        assetIndex: List<Int>,
    ): List<DownloadItem> {
        val params =
            mapOf(
                "hub_uuid" to hubUuid,
                "app_data" to appData,
                "hub_data" to hubData,
                "asset_index" to assetIndex,
            )
        return client.invoke("get_download", params, typeOf<List<DownloadItem>>())
    }

    // ========================================================================
    // Downloader RPC Methods
    // ========================================================================

    override suspend fun downloadSubmit(
        url: String,
        destPath: String,
        headers: Map<String, String>?,
        cookies: Map<String, String>?,
        hubUuid: String?,
    ): TaskIdResponse {
        val params =
            mutableMapOf<String, Any?>(
                "url" to url,
                "dest_path" to destPath,
            )
        if (headers != null) params["headers"] = headers
        if (cookies != null) params["cookies"] = cookies
        if (hubUuid != null) params["hub_uuid"] = hubUuid

        return client.invoke("download_submit", params, typeOf<TaskIdResponse>())
    }

    override suspend fun downloadGetStatus(taskId: String): TaskInfo {
        val params = mapOf("task_id" to taskId)
        return client.invoke("download_get_status", params, typeOf<TaskInfo>())
    }

    override suspend fun downloadWaitForChange(
        taskId: String,
        timeoutSeconds: Long,
    ): TaskInfo {
        val params =
            mapOf(
                "task_id" to taskId,
                "timeout_seconds" to timeoutSeconds,
            )
        // Use longer timeout for long-polling (add 5 seconds buffer)
        val rpcTimeout = (timeoutSeconds + 5) * 1000
        return client.invoke("download_wait_for_change", params, typeOf<TaskInfo>(), rpcTimeout)
    }

    override suspend fun downloadCancel(taskId: String): Boolean {
        val params = mapOf("task_id" to taskId)
        return client.invoke("download_cancel", params, typeOf<Boolean>())
    }

    override suspend fun downloadPause(taskId: String): Boolean {
        val params = mapOf("task_id" to taskId)
        return client.invoke("download_pause", params, typeOf<Boolean>())
    }

    override suspend fun downloadResume(taskId: String): Boolean {
        val params = mapOf("task_id" to taskId)
        return client.invoke("download_resume", params, typeOf<Boolean>())
    }

    // ========================================================================
    // External Downloader Registration
    // ========================================================================

    override suspend fun registerDownloader(
        hubUuid: String,
        rpcUrl: String,
    ): Boolean {
        val params =
            mapOf(
                "hub_uuid" to hubUuid,
                "rpc_url" to rpcUrl,
            )
        return client.invoke("register_downloader", params, typeOf<Boolean>())
    }

    override suspend fun unregisterDownloader(hubUuid: String): Boolean {
        val params = mapOf("hub_uuid" to hubUuid)
        return client.invoke("unregister_downloader", params, typeOf<Boolean>())
    }

    // ========================================================================
    // App Manager
    // ========================================================================

    override suspend fun managerGetApps(): List<AppRecord> = client.invoke("manager_get_apps", typeOf<List<AppRecord>>())

    override suspend fun managerSaveApp(record: AppRecord): AppRecord =
        client.invoke("manager_save_app", mapOf("record" to record), typeOf<AppRecord>())

    override suspend fun managerDeleteApp(recordId: String): Boolean =
        client.invoke("manager_delete_app", mapOf("record_id" to recordId), typeOf<Boolean>())

    override suspend fun managerGetAppStatus(recordId: String): AppStatus =
        client.invoke("manager_get_app_status", mapOf("record_id" to recordId), typeOf<AppStatus>())

    override suspend fun managerSetVirtualApps(apps: List<AppRecord>): Boolean =
        client.invoke("manager_set_virtual_apps", mapOf("apps" to apps), typeOf<Boolean>())

    override suspend fun managerRenewAll(): Boolean = client.invoke("manager_renew_all", typeOf<Boolean>())

    override suspend fun managerCheckInvalidApplications(): List<String> =
        client.invoke("manager_check_invalid_applications", typeOf<List<String>>())

    // ========================================================================
    // Hub Manager
    // ========================================================================

    override suspend fun managerGetHubs(): List<HubRecord> = client.invoke("manager_get_hubs", typeOf<List<HubRecord>>())

    override suspend fun managerSaveHub(record: HubRecord): Boolean =
        client.invoke("manager_save_hub", mapOf("record" to record), typeOf<Boolean>())

    override suspend fun managerDeleteHub(hubUuid: String): Boolean =
        client.invoke("manager_delete_hub", mapOf("hub_uuid" to hubUuid), typeOf<Boolean>())

    override suspend fun managerUpdateHubAuth(hubUuid: String, auth: Map<String, String>): Boolean =
        client.invoke(
            "manager_update_hub_auth",
            mapOf("hub_uuid" to hubUuid, "auth" to auth),
            typeOf<Boolean>(),
        )

    override suspend fun managerHubIgnoreApp(
        hubUuid: String,
        appId: Map<String, String?>,
        ignore: Boolean,
    ): Boolean =
        client.invoke(
            "manager_hub_ignore_app",
            mapOf("hub_uuid" to hubUuid, "app_id" to appId, "ignore" to ignore),
            typeOf<Boolean>(),
        )

    override suspend fun managerSetApplicationsMode(
        hubUuid: String,
        enable: Boolean,
    ): Boolean =
        client.invoke(
            "manager_set_applications_mode",
            mapOf("hub_uuid" to hubUuid, "enable" to enable),
            typeOf<Boolean>(),
        )

    // ========================================================================
    // Extra Hub
    // ========================================================================

    override suspend fun managerGetExtraHubs(): List<ExtraHubRecord> =
        client.invoke("manager_get_extra_hubs", typeOf<List<ExtraHubRecord>>())

    override suspend fun managerSaveExtraHub(record: ExtraHubRecord): Boolean =
        client.invoke("manager_save_extra_hub", mapOf("record" to record), typeOf<Boolean>())

    override suspend fun managerDeleteExtraHub(id: String): Boolean =
        client.invoke("manager_delete_extra_hub", mapOf("id" to id), typeOf<Boolean>())

    // ========================================================================
    // Extra App
    // ========================================================================

    override suspend fun managerGetExtraAppByAppId(appId: Map<String, String?>): ExtraAppRecord? =
        client.invoke("manager_get_extra_app_by_app_id", mapOf("app_id" to appId), typeOf<ExtraAppRecord?>())

    override suspend fun managerSaveExtraApp(record: ExtraAppRecord): Boolean =
        client.invoke("manager_save_extra_app", mapOf("record" to record), typeOf<Boolean>())

    override suspend fun managerDeleteExtraApp(id: String): Boolean =
        client.invoke("manager_delete_extra_app", mapOf("id" to id), typeOf<Boolean>())

    // ========================================================================
    // Android API / Notification Registration
    // ========================================================================

    override suspend fun registerAndroidApi(url: String): Boolean =
        client.invoke("register_android_api", mapOf("url" to url), typeOf<Boolean>())

    override suspend fun registerNotification(url: String): Boolean =
        client.invoke("register_notification", mapOf("url" to url), typeOf<Boolean>())

    // ========================================================================
    // Cloud Config Manager
    // ========================================================================

    override suspend fun cloudConfigInit(apiUrl: String): Boolean =
        client.invoke("cloud_config_init", mapOf("api_url" to apiUrl), typeOf<Boolean>())

    override suspend fun cloudConfigRenew(): Boolean = client.invoke("cloud_config_renew", typeOf<Boolean>())

    override suspend fun cloudConfigGetAppList(): List<AppConfig> = client.invoke("cloud_config_get_app_list", typeOf<List<AppConfig>>())

    override suspend fun cloudConfigGetHubList(): List<HubConfig> = client.invoke("cloud_config_get_hub_list", typeOf<List<HubConfig>>())

    override suspend fun cloudConfigApplyApp(uuid: String): Boolean =
        client.invoke("cloud_config_apply_app", mapOf("uuid" to uuid), typeOf<Boolean>())

    override suspend fun cloudConfigApplyHub(uuid: String): Boolean =
        client.invoke("cloud_config_apply_hub", mapOf("uuid" to uuid), typeOf<Boolean>())

    override suspend fun cloudConfigRenewAll(): Boolean = client.invoke("cloud_config_renew_all", typeOf<Boolean>())

    /**
     * Close the underlying WebSocket connection
     */
    suspend fun close() {
        client.close()
    }
}
