package net.xzos.upgradeall

import org.json.JSONObject

object GetterBridgeRequestBuilder {
    fun runtimeOperationRequest(args: Map<*, *>): String {
        val operation = args["operation"] as? String
            ?: throw IllegalArgumentException("operation is required")
        val payload = args["payload"] as? Map<*, *> ?: emptyMap<Any?, Any?>()
        return JSONObject()
            .put("operation", operation)
            .put("payload", JSONObject(payload))
            .toString()
    }
}
