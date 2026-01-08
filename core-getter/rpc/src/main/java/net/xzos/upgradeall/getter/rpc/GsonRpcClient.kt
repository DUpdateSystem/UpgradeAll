package net.xzos.upgradeall.getter.rpc

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import java.io.OutputStreamWriter
import java.lang.reflect.Type
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicLong

/**
 * A JSON-RPC 2.0 client using Gson for serialization/deserialization.
 * This replaces jsonrpc4j to ensure consistent use of Gson annotations (@SerializedName).
 */
class GsonRpcClient(private val url: String) {
    private val gson = Gson()
    private val requestId = AtomicLong(1)

    /**
     * Invoke a JSON-RPC method with named parameters
     */
    fun <T> invoke(method: String, params: Map<String, Any?>, resultType: Type): T {
        val request = JsonObject().apply {
            addProperty("jsonrpc", "2.0")
            addProperty("method", method)
            addProperty("id", requestId.getAndIncrement())
            add("params", gson.toJsonTree(params))
        }

        val responseJson = sendRequest(request)
        return parseResponse(responseJson, resultType)
    }

    /**
     * Invoke a JSON-RPC method without parameters
     */
    fun <T> invoke(method: String, resultType: Type): T {
        val request = JsonObject().apply {
            addProperty("jsonrpc", "2.0")
            addProperty("method", method)
            addProperty("id", requestId.getAndIncrement())
        }

        val responseJson = sendRequest(request)
        return parseResponse(responseJson, resultType)
    }

    /**
     * Invoke a JSON-RPC method that returns nothing (notification style but with id)
     */
    fun invokeVoid(method: String, params: Map<String, Any?> = emptyMap()) {
        val request = JsonObject().apply {
            addProperty("jsonrpc", "2.0")
            addProperty("method", method)
            addProperty("id", requestId.getAndIncrement())
            if (params.isNotEmpty()) {
                add("params", gson.toJsonTree(params))
            }
        }

        val responseJson = sendRequest(request)
        // Check for errors even if we don't need the result
        val response = gson.fromJson(responseJson, JsonObject::class.java)
        if (response.has("error") && !response.get("error").isJsonNull) {
            val error = response.getAsJsonObject("error")
            val message = error.get("message")?.asString ?: "Unknown error"
            val data = error.get("data")?.asString
            throw RpcException(message, data)
        }
    }

    private fun sendRequest(request: JsonObject): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.connectTimeout = 30000
            connection.readTimeout = 60000

            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(request.toString())
            }

            val responseCode = connection.responseCode
            val responseBody = if (responseCode in 200..299) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                connection.errorStream?.bufferedReader()?.use { it.readText() }
                    ?: "HTTP $responseCode"
            }

            if (responseCode !in 200..299) {
                throw RpcException("HTTP error $responseCode", responseBody)
            }

            return responseBody
        } finally {
            connection.disconnect()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> parseResponse(responseJson: String, resultType: Type): T {
        val response = gson.fromJson(responseJson, JsonObject::class.java)

        // Check for error
        if (response.has("error") && !response.get("error").isJsonNull) {
            val error = response.getAsJsonObject("error")
            val message = error.get("message")?.asString ?: "Unknown error"
            val data = error.get("data")?.asString
            throw RpcException(message, data)
        }

        // Parse result
        val result = response.get("result")
        return gson.fromJson(result, resultType) as T
    }
}

class RpcException(message: String, val data: String? = null) : RuntimeException(
    if (data != null) "$message: $data" else message
)

// Type token helpers for generic types
inline fun <reified T> typeOf(): Type = object : TypeToken<T>() {}.type
