package net.xzos.upgradeall

import org.json.JSONArray
import org.json.JSONObject

object GetterBridgeRequestBuilder {
    fun startupRequest(dataDir: String, args: Map<*, *>): String {
        val scanOptions = args["scan_options"] as? Map<*, *> ?: emptyMap<Any?, Any?>()
        return JSONObject()
            .put("data_dir", dataDir)
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

    fun freshInstallSetupPreviewRequest(args: Map<*, *>): String {
        requireOnlyKeys(args, setOf("scan_options"))
        val scanOptions = args["scan_options"] as? Map<*, *> ?: emptyMap<Any?, Any?>()
        requireOnlyKeys(scanOptions, setOf("include_system_apps", "include_self"))
        return installedAutogenPreviewRequest(mapOf("scan_options" to scanOptions))
    }

    fun freshInstallSetupApplyRequest(args: Map<*, *>): String {
        requireOnlyKeys(args, setOf("preview_id", "accepted_package_ids", "accept_all"))
        val previewId = (args["preview_id"] as? String)
            ?.takeIf(String::isNotBlank)
            ?: throw IllegalArgumentException("preview_id is required")
        val accepted = args["accepted_package_ids"] as? List<*>
        val acceptAll = args["accept_all"] as? Boolean
        if ((accepted != null) == (acceptAll == true)) {
            throw IllegalArgumentException(
                "exactly one of accepted_package_ids or accept_all is required",
            )
        }
        val packageIds = accepted?.map { value ->
            (value as? String)?.takeIf(String::isNotBlank)
                ?: throw IllegalArgumentException("accepted_package_ids must contain non-empty strings")
        }
        return JSONObject()
            .put("preview_id", previewId)
            .also { request ->
                if (packageIds != null) {
                    request.put("accepted_package_ids", JSONArray(packageIds))
                } else {
                    request.put("accept_all", true)
                }
            }
            .toString()
    }

    fun fdroidCatalogRefreshRequest(dataDir: String): String = JSONObject()
        .put("data_dir", dataDir)
        .toString()

    fun githubAutogenPreviewRequest(args: Map<*, *>): String {
        val owner = args["owner"] as? String
            ?: throw IllegalArgumentException("owner is required")
        val repo = args["repo"] as? String
            ?: throw IllegalArgumentException("repo is required")
        val androidPackage = args["android_package"] as? String
            ?: throw IllegalArgumentException("android_package is required")
        val displayName = args["display_name"] as? String
        val request = JSONObject()
            .put("owner", owner)
            .put("repo", repo)
            .put("android_package", androidPackage)
        if (!displayName.isNullOrBlank()) {
            request.put("display_name", displayName)
        }
        return request.toString()
    }

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

    private fun requireOnlyKeys(args: Map<*, *>, allowed: Set<String>) {
        val invalid = args.keys.firstOrNull { key -> key !is String || key !in allowed }
        if (invalid != null) {
            throw IllegalArgumentException("$invalid is not allowed")
        }
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
