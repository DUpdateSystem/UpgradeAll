package net.xzos.upgradeall.getter.rpc

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.lang.reflect.Type
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.time.Duration.Companion.seconds

/**
 * WebSocket-based JSON-RPC 2.0 client using Ktor.
 * 
 * This client maintains a persistent WebSocket connection and handles concurrent
 * JSON-RPC requests by matching request IDs with responses.
 */
class RpcClient(private val url: String) {
    private val gson = Gson()
    private val requestId = AtomicLong(1)
    
    private val client = HttpClient(CIO) {
        install(WebSockets) {
            pingInterval = 30.seconds
        }
    }
    
    private var sessionJob: Job? = null
    private var session: DefaultClientWebSocketSession? = null
    private val pendingRequests = ConcurrentHashMap<Long, CompletableDeferred<JsonObject>>()
    private val sessionMutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    @Volatile
    private var isConnected = false
    
    /**
     * Ensure WebSocket connection is established
     */
    private suspend fun ensureConnected() {
        sessionMutex.withLock {
            if (isConnected && session != null) return@withLock
            
            // Close old session if exists
            session?.close()
            sessionJob?.cancel()
            
            // Parse URL (format: "ws://host:port" or "http://host:port")
            val wsUrl = url.replace("http://", "ws://").replace("https://", "wss://")
            
            try {
                sessionJob = scope.launch {
                    client.webSocket(wsUrl) {
                        session = this
                        isConnected = true
                        
                        // Start message receiver loop
                        for (frame in incoming) {
                            if (frame is Frame.Text) {
                                val text = frame.readText()
                                handleResponse(text)
                            }
                        }
                    }
                    // Connection closed
                    isConnected = false
                    session = null
                    
                    // Fail all pending requests
                    val exception = RpcException("WebSocket connection closed")
                    pendingRequests.values.forEach { it.completeExceptionally(exception) }
                    pendingRequests.clear()
                }
                
                // Wait for connection to be established
                var attempts = 0
                while (!isConnected && attempts < 50) {
                    delay(100)
                    attempts++
                }
                
                if (!isConnected) {
                    throw RpcException("Failed to connect to WebSocket server")
                }
            } catch (e: Exception) {
                isConnected = false
                session = null
                throw RpcException("WebSocket connection error: ${e.message}", e.toString())
            }
        }
    }
    
    /**
     * Handle incoming JSON-RPC response
     */
    private fun handleResponse(text: String) {
        try {
            val response = gson.fromJson(text, JsonObject::class.java)
            
            if (response.has("id") && !response.get("id").isJsonNull) {
                val id = response.get("id").asLong
                val deferred = pendingRequests.remove(id)
                
                if (deferred != null) {
                    if (response.has("error") && !response.get("error").isJsonNull) {
                        val error = response.getAsJsonObject("error")
                        val message = error.get("message")?.asString ?: "Unknown error"
                        val data = error.get("data")?.asString
                        deferred.completeExceptionally(RpcException(message, data))
                    } else {
                        deferred.complete(response)
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore malformed responses
        }
    }
    
    /**
     * Invoke a JSON-RPC method with named parameters
     */
    suspend fun <T> invoke(method: String, params: Map<String, Any?>, resultType: Type, timeoutMillis: Long = 60_000): T {
        ensureConnected()
        
        val id = requestId.getAndIncrement()
        val request = JsonObject().apply {
            addProperty("jsonrpc", "2.0")
            addProperty("method", method)
            addProperty("id", id)
            add("params", gson.toJsonTree(params))
        }
        
        val deferred = CompletableDeferred<JsonObject>()
        pendingRequests[id] = deferred
        
        return try {
            // Send request
            session?.send(Frame.Text(request.toString()))
                ?: throw RpcException("WebSocket session is null")
            
            // Wait for response with timeout
            val response = withTimeout(timeoutMillis) {
                deferred.await()
            }
            
            // Parse result
            val result = response.get("result")
            @Suppress("UNCHECKED_CAST")
            gson.fromJson(result, resultType) as T
        } catch (e: TimeoutCancellationException) {
            pendingRequests.remove(id)
            throw RpcException("Request timeout after ${timeoutMillis}ms", null)
        } catch (e: RpcException) {
            throw e
        } catch (e: Exception) {
            pendingRequests.remove(id)
            throw RpcException("Request failed: ${e.message}", e.toString())
        }
    }
    
    /**
     * Invoke a JSON-RPC method without parameters
     */
    suspend fun <T> invoke(method: String, resultType: Type, timeoutMillis: Long = 60_000): T {
        ensureConnected()
        
        val id = requestId.getAndIncrement()
        val request = JsonObject().apply {
            addProperty("jsonrpc", "2.0")
            addProperty("method", method)
            addProperty("id", id)
        }
        
        val deferred = CompletableDeferred<JsonObject>()
        pendingRequests[id] = deferred
        
        return try {
            // Send request
            session?.send(Frame.Text(request.toString()))
                ?: throw RpcException("WebSocket session is null")
            
            // Wait for response with timeout
            val response = withTimeout(timeoutMillis) {
                deferred.await()
            }
            
            // Parse result
            val result = response.get("result")
            @Suppress("UNCHECKED_CAST")
            gson.fromJson(result, resultType) as T
        } catch (e: TimeoutCancellationException) {
            pendingRequests.remove(id)
            throw RpcException("Request timeout after ${timeoutMillis}ms", null)
        } catch (e: RpcException) {
            throw e
        } catch (e: Exception) {
            pendingRequests.remove(id)
            throw RpcException("Request failed: ${e.message}", e.toString())
        }
    }
    
    /**
     * Invoke a JSON-RPC method that returns nothing
     */
    suspend fun invokeVoid(method: String, params: Map<String, Any?> = emptyMap(), timeoutMillis: Long = 60_000) {
        ensureConnected()
        
        val id = requestId.getAndIncrement()
        val request = JsonObject().apply {
            addProperty("jsonrpc", "2.0")
            addProperty("method", method)
            addProperty("id", id)
            if (params.isNotEmpty()) {
                add("params", gson.toJsonTree(params))
            }
        }
        
        val deferred = CompletableDeferred<JsonObject>()
        pendingRequests[id] = deferred
        
        try {
            // Send request
            session?.send(Frame.Text(request.toString()))
                ?: throw RpcException("WebSocket session is null")
            
            // Wait for response with timeout
            val response = withTimeout(timeoutMillis) {
                deferred.await()
            }
            
            // Check for errors
            if (response.has("error") && !response.get("error").isJsonNull) {
                val error = response.getAsJsonObject("error")
                val message = error.get("message")?.asString ?: "Unknown error"
                val data = error.get("data")?.asString
                throw RpcException(message, data)
            }
        } catch (e: TimeoutCancellationException) {
            pendingRequests.remove(id)
            throw RpcException("Request timeout after ${timeoutMillis}ms", null)
        } catch (e: RpcException) {
            throw e
        } catch (e: Exception) {
            pendingRequests.remove(id)
            throw RpcException("Request failed: ${e.message}", e.toString())
        }
    }
    
    /**
     * Close the WebSocket connection
     */
    suspend fun close() {
        sessionMutex.withLock {
            isConnected = false
            session?.close()
            session = null
            sessionJob?.cancel()
            sessionJob = null
            scope.cancel()
            client.close()
            
            // Fail all pending requests
            val exception = RpcException("Client closed")
            pendingRequests.values.forEach { it.completeExceptionally(exception) }
            pendingRequests.clear()
        }
    }
}

/**
 * JSON-RPC exception thrown when RPC call fails
 */
class RpcException(message: String, val data: String? = null) : RuntimeException(
    if (data != null) "$message: $data" else message
)

/**
 * Type token helper for generic types with Gson
 */
inline fun <reified T> typeOf(): Type = object : TypeToken<T>() {}.type
