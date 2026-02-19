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
import net.xzos.upgradeall.getter.rpc.*

/**
 * Lightweight HTTP JSON-RPC 2.0 server that exposes a downloader implementation
 * to the Rust getter via ExternalRpcDownloader.
 *
 * Binds to 127.0.0.1 on a random port (local-only).
 *
 * Supported RPC methods (matching Rust downloader protocol):
 * - download_submit:           url, dest_path, headers?, cookies? -> {task_id}
 * - download_get_status:       task_id -> TaskInfo
 * - download_wait_for_change:  task_id, timeout_seconds -> TaskInfo
 * - download_pause:            task_id -> bool
 * - download_resume:           task_id -> bool
 * - download_cancel:           task_id -> bool
 */
class KotlinDownloaderRpcServer(
    private val downloader: DownloaderImpl,
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
                                this@KotlinDownloaderRpcServer.handleJsonRpc(body)
                            }
                        call.respondText(response, ContentType.Application.Json, HttpStatusCode.OK)
                    }
                }
            }
        val started = srv.start(wait = false)
        stopAction = { started.stop(1000, 2000) }
        val serverPort =
            started.engine
                .resolvedConnectors()
                .first()
                .port
        val url = "http://127.0.0.1:$serverPort"
        println("KotlinDownloaderRpcServer started at $url")
        return url
    }

    /**
     * Stop the server.
     */
    fun stop() {
        stopAction?.invoke()
        stopAction = null
    }

    private suspend fun handleJsonRpc(body: String): String {
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
                "download_submit" -> handleDownloadSubmit(id, params)
                "download_get_status" -> handleDownloadGetStatus(id, params)
                "download_wait_for_change" -> handleDownloadWaitForChange(id, params)
                "download_pause" -> handleDownloadPause(id, params)
                "download_resume" -> handleDownloadResume(id, params)
                "download_cancel" -> handleDownloadCancel(id, params)
                else -> jsonRpcError(id, -32601, "Method not found: $method")
            }
        } catch (e: Throwable) {
            println("RPC method '$method' failed: ${e.message}")
            e.printStackTrace()
            jsonRpcError(id, -32603, "Internal error: ${e.message}")
        }
    }

    // ========================================================================
    // RPC Method Handlers
    // ========================================================================

    private suspend fun handleDownloadSubmit(
        id: JsonElement?,
        params: JsonObject,
    ): String {
        val url = params.get("url")?.asString ?: return jsonRpcError(id, -32602, "Missing url")
        val destPath = params.get("dest_path")?.asString 
            ?: return jsonRpcError(id, -32602, "Missing dest_path")
        
        val headers = parseStringMap(params.getAsJsonObject("headers"))
        val cookies = parseStringMap(params.getAsJsonObject("cookies"))

        val taskId = downloader.submitDownload(url, destPath, headers, cookies)
        
        val result = JsonObject()
        result.addProperty("task_id", taskId)
        return jsonRpcSuccess(id, result)
    }

    private suspend fun handleDownloadGetStatus(
        id: JsonElement?,
        params: JsonObject,
    ): String {
        val taskId = params.get("task_id")?.asString 
            ?: return jsonRpcError(id, -32602, "Missing task_id")
        
        val taskInfo = downloader.getStatus(taskId)
            ?: return jsonRpcError(id, -32602, "Task not found: $taskId")
        
        return jsonRpcSuccess(id, gson.toJsonTree(taskInfo))
    }

    private suspend fun handleDownloadWaitForChange(
        id: JsonElement?,
        params: JsonObject,
    ): String {
        val taskId = params.get("task_id")?.asString 
            ?: return jsonRpcError(id, -32602, "Missing task_id")
        val timeoutSeconds = params.get("timeout_seconds")?.asLong ?: 30L
        
        val taskInfo = downloader.waitForChange(taskId, timeoutSeconds)
            ?: return jsonRpcError(id, -32602, "Task not found: $taskId")
        
        return jsonRpcSuccess(id, gson.toJsonTree(taskInfo))
    }

    private suspend fun handleDownloadPause(
        id: JsonElement?,
        params: JsonObject,
    ): String {
        val taskId = params.get("task_id")?.asString 
            ?: return jsonRpcError(id, -32602, "Missing task_id")
        
        val success = downloader.pause(taskId)
        return jsonRpcSuccess(id, gson.toJsonTree(success))
    }

    private suspend fun handleDownloadResume(
        id: JsonElement?,
        params: JsonObject,
    ): String {
        val taskId = params.get("task_id")?.asString 
            ?: return jsonRpcError(id, -32602, "Missing task_id")
        
        val success = downloader.resume(taskId)
        return jsonRpcSuccess(id, gson.toJsonTree(success))
    }

    private suspend fun handleDownloadCancel(
        id: JsonElement?,
        params: JsonObject,
    ): String {
        val taskId = params.get("task_id")?.asString 
            ?: return jsonRpcError(id, -32602, "Missing task_id")
        
        val success = downloader.cancel(taskId)
        return jsonRpcSuccess(id, gson.toJsonTree(success))
    }

    // ========================================================================
    // Helpers
    // ========================================================================

    private fun parseStringMap(json: JsonObject?): Map<String, String> {
        if (json == null) return emptyMap()
        return json.entrySet().associate { (key, value) ->
            key to (if (value.isJsonNull) "" else value.asString)
        }
    }

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
}

/**
 * Interface that downloader implementations must provide
 */
interface DownloaderImpl {
    suspend fun submitDownload(
        url: String,
        destPath: String,
        headers: Map<String, String>,
        cookies: Map<String, String>
    ): String
    
    suspend fun getStatus(taskId: String): TaskInfo?
    suspend fun waitForChange(taskId: String, timeoutSeconds: Long): TaskInfo?
    suspend fun pause(taskId: String): Boolean
    suspend fun resume(taskId: String): Boolean
    suspend fun cancel(taskId: String): Boolean
}
