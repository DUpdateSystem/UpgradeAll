package net.xzos.upgradeall

import org.json.JSONArray
import org.json.JSONObject

object GetterBridgeRequestBuilder {
    fun readOperationRequest(args: Map<*, *>): String = operationRequest(args)

    fun runtimeOperationRequest(args: Map<*, *>): String = operationRequest(args)

    fun installedAutogenPreviewRequest(args: Map<*, *>): String {
        val scanOptions = args["scan_options"] as? Map<*, *> ?: args
        return JSONObject()
            .put(
                "scan_options",
                JSONObject()
                    .put(
                        "include_system_apps",
                        scanOptions["include_system_apps"] as? Boolean ?: false,
                    )
                    .put(
                        "include_self",
                        scanOptions["include_self"] as? Boolean ?: false,
                    ),
            )
            .toString()
    }

    fun fdroidCatalogRefreshRequest(dataDir: String): String = JSONObject()
        .put("data_dir", dataDir)
        .toString()

    fun fdroidAutogenPreviewRequest(args: Map<*, *>): String {
        val payload = args["payload"] as? Map<*, *> ?: emptyMap<Any?, Any?>()
        return JSONObject()
            .put("payload", JSONObject(payload))
            .toString()
    }

    fun autogenApplyRequest(args: Map<*, *>): String {
        val previewJson = args["preview_json"] as? String
            ?: throw IllegalArgumentException("preview_json is required")
        val acceptance = args["acceptance"] as? Map<*, *>
        val packageIds = acceptance
            ?.get("package_ids")
            ?.let { value -> value as? Collection<*> }
            ?.map { value -> value.toString() }
            ?: emptyList<String>()
        return JSONObject()
            .put("preview", JSONObject(previewJson))
            .put(
                "acceptance",
                JSONObject()
                    .put("mode", acceptance?.get("mode") as? String ?: "all")
                    .put("package_ids", JSONArray(packageIds)),
            )
            .toString()
    }

    private fun operationRequest(args: Map<*, *>): String {
        val operation = args["operation"] as? String
            ?: throw IllegalArgumentException("operation is required")
        val payload = args["payload"] as? Map<*, *> ?: emptyMap<Any?, Any?>()
        return JSONObject()
            .put("operation", operation)
            .put("payload", JSONObject(payload))
            .toString()
    }
}
