package net.xzos.upgradeall.getter.rpc

import net.xzos.upgradeall.websdk.data.json.CloudConfigList
import net.xzos.upgradeall.websdk.data.json.ReleaseGson

/**
 * WebSocket-based implementation of GetterService.
 * 
 * Uses RpcClient internally to communicate with the Rust getter server
 * over a persistent WebSocket connection.
 */
class GetterServiceImpl(private val client: RpcClient) : GetterService {
    
    override suspend fun ping(): String {
        return client.invoke("ping", typeOf<String>())
    }

    override suspend fun init(
        dataPath: String,
        cachePath: String,
        globalExpireTime: Long
    ): Boolean {
        val params = mapOf(
            "data_path" to dataPath,
            "cache_path" to cachePath,
            "global_expire_time" to globalExpireTime
        )
        return client.invoke("init", params, typeOf<Boolean>())
    }

    override suspend fun shutdown() {
        client.invokeVoid("shutdown")
    }

    override suspend fun checkAppAvailable(
        hubUuid: String,
        appData: Map<String, String>,
        hubData: Map<String, String>
    ): Boolean {
        val params = mapOf(
            "hub_uuid" to hubUuid,
            "app_data" to appData,
            "hub_data" to hubData
        )
        return client.invoke("check_app_available", params, typeOf<Boolean>())
    }

    override suspend fun getAppLatestRelease(
        hubUuid: String,
        appData: Map<String, String>,
        hubData: Map<String, String>
    ): ReleaseGson {
        val params = mapOf(
            "hub_uuid" to hubUuid,
            "app_data" to appData,
            "hub_data" to hubData
        )
        return client.invoke("get_latest_release", params, typeOf<ReleaseGson>())
    }

    override suspend fun getAppReleases(
        hubUuid: String,
        appData: Map<String, String>,
        hubData: Map<String, String>
    ): List<ReleaseGson> {
        val params = mapOf(
            "hub_uuid" to hubUuid,
            "app_data" to appData,
            "hub_data" to hubData
        )
        return client.invoke("get_releases", params, typeOf<List<ReleaseGson>>())
    }

    override suspend fun getCloudConfig(url: String): CloudConfigList {
        val params = mapOf("url" to url)
        return client.invoke("get_cloud_config", params, typeOf<CloudConfigList>())
    }

    // ========================================================================
    // Downloader RPC Methods
    // ========================================================================

    override suspend fun downloadSubmit(
        url: String,
        destPath: String,
        headers: Map<String, String>?,
        cookies: Map<String, String>?
    ): TaskIdResponse {
        val params = mutableMapOf<String, Any?>(
            "url" to url,
            "dest_path" to destPath
        )
        if (headers != null) params["headers"] = headers
        if (cookies != null) params["cookies"] = cookies
        
        return client.invoke("download_submit", params, typeOf<TaskIdResponse>())
    }

    override suspend fun downloadGetStatus(taskId: String): TaskInfo {
        val params = mapOf("task_id" to taskId)
        return client.invoke("download_get_status", params, typeOf<TaskInfo>())
    }

    override suspend fun downloadWaitForChange(
        taskId: String,
        timeoutSeconds: Long
    ): TaskInfo {
        val params = mapOf(
            "task_id" to taskId,
            "timeout_seconds" to timeoutSeconds
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
    
    /**
     * Close the underlying WebSocket connection
     */
    suspend fun close() {
        client.close()
    }
}
