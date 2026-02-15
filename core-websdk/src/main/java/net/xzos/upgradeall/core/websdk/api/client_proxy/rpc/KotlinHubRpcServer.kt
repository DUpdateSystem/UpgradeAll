package net.xzos.upgradeall.core.websdk.api.client_proxy.rpc

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.request.receiveText
import io.ktor.server.response.respondText
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.xzos.upgradeall.core.utils.log.Log
import net.xzos.upgradeall.core.utils.log.ObjectTag
import net.xzos.upgradeall.core.utils.log.ObjectTag.Companion.core
import net.xzos.upgradeall.core.websdk.api.client_proxy.hubs.BaseHub
import net.xzos.upgradeall.core.websdk.base_model.AppData
import net.xzos.upgradeall.core.websdk.base_model.HubData
import net.xzos.upgradeall.websdk.data.json.ReleaseGson

/**
 * Lightweight HTTP JSON-RPC 2.0 server that exposes Kotlin-based hub implementations
 * (GooglePlay, CoolApk, etc.) so the Rust getter can invoke them via OutsideProvider.
 *
 * Binds to 127.0.0.1 on a random port (local-only).
 *
 * Supported RPC methods:
 * - check_app_available: hub_uuid, app_data, hub_data -> bool
 * - get_latest_release:  hub_uuid, app_data, hub_data -> ReleaseGson
 * - get_releases:        hub_uuid, app_data, hub_data -> List of ReleaseGson
 * - get_download:        hub_uuid, app_data, hub_data, asset_index -> List of DownloadItem
 */
class KotlinHubRpcServer(
    private val hubs: Map<String, BaseHub>,
) {
    private var stopAction: (() -> Unit)? = null
    private val gson = Gson()

    /**
     * Start the HTTP JSON-RPC server and return its URL (e.g., "http://127.0.0.1:12345").
     */
    suspend fun start(): String {
        val srv =
            embeddedServer(CIO, port = 0, host = "127.0.0.1") {
                routing {
                    post("/") {
                        val body = call.receiveText()
                        val response =
                            withContext(Dispatchers.IO) {
                                handleJsonRpc(body)
                            }
                        call.respondText(response, ContentType.Application.Json, HttpStatusCode.OK)
                    }
                }
            }
        val started = srv.start(wait = false)
        stopAction = { started.stop(1000, 2000) }
        val port =
            started.engine
                .resolvedConnectors()
                .first()
                .port
        val url = "http://127.0.0.1:$port"
        Log.i(logObjectTag, TAG, "KotlinHubRpcServer started at $url")
        return url
    }

    /**
     * Stop the server.
     */
    fun stop() {
        stopAction?.invoke()
        stopAction = null
    }

    /**
     * List of hub UUIDs this server handles.
     */
    fun getHubUuids(): List<String> = hubs.keys.toList()

    private fun handleJsonRpc(body: String): String {
        val request: JsonObject
        try {
            request = gson.fromJson(body, JsonObject::class.java)
        } catch (e: Exception) {
            return jsonRpcError(null, -32700, "Parse error: ${e.message}")
        }

        val id = request.get("id")
        val method =
            request.get("method")?.asString
                ?: return jsonRpcError(id, -32600, "Missing method")
        val params =
            request.getAsJsonObject("params")
                ?: return jsonRpcError(id, -32600, "Missing params")

        return try {
            when (method) {
                "check_app_available" -> handleCheckAppAvailable(id, params)
                "get_latest_release" -> handleGetLatestRelease(id, params)
                "get_releases" -> handleGetReleases(id, params)
                "get_download" -> handleGetDownload(id, params)
                else -> jsonRpcError(id, -32601, "Method not found: $method")
            }
        } catch (e: Throwable) {
            Log.e(logObjectTag, TAG, "RPC method '$method' failed: ${e.message}")
            jsonRpcError(id, -32603, "Internal error: ${e.message}")
        }
    }

    // ========================================================================
    // RPC Method Handlers
    // ========================================================================

    private fun handleCheckAppAvailable(
        id: JsonElement?,
        params: JsonObject,
    ): String {
        val (hub, hubData, appData) =
            parseCommonParams(params)
                ?: return jsonRpcError(id, -32602, "Hub not found")
        val result = hub.checkAppAvailable(hubData, appData)
        return jsonRpcSuccess(id, gson.toJsonTree(result ?: false))
    }

    private fun handleGetLatestRelease(
        id: JsonElement?,
        params: JsonObject,
    ): String {
        val (hub, hubData, appData) =
            parseCommonParams(params)
                ?: return jsonRpcError(id, -32602, "Hub not found")
        val releases = hub.getReleases(hubData, appData)
        val latest = releases?.firstOrNull()
        return if (latest != null) {
            jsonRpcSuccess(id, toRustReleaseJson(latest))
        } else {
            jsonRpcError(id, -32604, "No releases found")
        }
    }

    private fun handleGetReleases(
        id: JsonElement?,
        params: JsonObject,
    ): String {
        val (hub, hubData, appData) =
            parseCommonParams(params)
                ?: return jsonRpcError(id, -32602, "Hub not found")
        val releases = hub.getReleases(hubData, appData) ?: emptyList()
        return jsonRpcSuccess(id, gson.toJsonTree(releases))
    }

    private fun handleGetDownload(
        id: JsonElement?,
        params: JsonObject,
    ): String {
        val (hub, hubData, appData) =
            parseCommonParams(params)
                ?: return jsonRpcError(id, -32602, "Hub not found")
        val assetIndexArray = params.getAsJsonArray("asset_index")
        val assetIndex = assetIndexArray?.map { it.asInt } ?: emptyList()

        // Get the asset from releases to pass to getDownload
        val releases = hub.getReleases(hubData, appData)
        val asset =
            if (assetIndex.size >= 2) {
                releases?.getOrNull(assetIndex[0])?.assetGsonList?.getOrNull(assetIndex[1])
            } else {
                null
            }

        val downloads = hub.getDownload(hubData, appData, assetIndex, asset) ?: emptyList()
        return jsonRpcSuccess(id, gson.toJsonTree(downloads))
    }

    // ========================================================================
    // Parameter Parsing Helpers
    // ========================================================================

    private data class HubContext(
        val hub: BaseHub,
        val hubData: HubData,
        val appData: AppData,
    )

    private fun parseCommonParams(params: JsonObject): HubContext? {
        val hubUuid = params.get("hub_uuid")?.asString ?: return null
        val hub = hubs[hubUuid] ?: return null

        val appDataMap = parseStringMap(params.getAsJsonObject("app_data"))
        val hubDataMap = parseStringMap(params.getAsJsonObject("hub_data"))

        val hubData =
            HubData(
                hubUuid = hubUuid,
                auth = hubDataMap,
                other = emptyMap(),
            )
        val appData =
            AppData(
                appId = appDataMap,
                other = emptyMap(),
            )

        return HubContext(hub, hubData, appData)
    }

    private fun parseStringMap(json: JsonObject?): Map<String, String?> {
        if (json == null) return emptyMap()
        return json.entrySet().associate { (key, value) ->
            key to if (value.isJsonNull) null else value.asString
        }
    }

    // ========================================================================
    // JSON Conversion: Kotlin ReleaseGson -> Rust ReleaseData format
    // ========================================================================

    /**
     * Convert Kotlin's [ReleaseGson] to the JSON format expected by Rust's ReleaseData.
     * Since ReleaseGson uses @SerializedName with snake_case (matching Rust's serde naming),
     * Gson serialization produces the correct format directly.
     *
     * Rust expects: version_number, changelog, assets with file_name, file_type, download_url, extra
     */
    private fun toRustReleaseJson(release: ReleaseGson): JsonElement = gson.toJsonTree(release)

    // ========================================================================
    // JSON-RPC Response Helpers
    // ========================================================================

    private fun jsonRpcSuccess(
        id: JsonElement?,
        result: JsonElement,
    ): String {
        val response = JsonObject()
        response.addProperty("jsonrpc", "2.0")
        response.add("result", result)
        response.add("id", id)
        return response.toString()
    }

    private fun jsonRpcError(
        id: JsonElement?,
        code: Int,
        message: String,
    ): String {
        val response = JsonObject()
        response.addProperty("jsonrpc", "2.0")
        val error = JsonObject()
        error.addProperty("code", code)
        error.addProperty("message", message)
        response.add("error", error)
        response.add("id", id)
        return response.toString()
    }

    companion object {
        private const val TAG = "KotlinHubRpcServer"
        private val logObjectTag = ObjectTag(core, TAG)
    }
}
