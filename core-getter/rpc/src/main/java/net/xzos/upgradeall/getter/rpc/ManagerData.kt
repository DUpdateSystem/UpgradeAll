package net.xzos.upgradeall.getter.rpc

import com.google.gson.annotations.SerializedName

// ============================================================================
// App Status
// ============================================================================

enum class AppStatus {
    @SerializedName("app_inactive")
    APP_INACTIVE,

    @SerializedName("app_pending")
    APP_PENDING,

    @SerializedName("network_error")
    NETWORK_ERROR,

    @SerializedName("app_latest")
    APP_LATEST,

    @SerializedName("app_outdated")
    APP_OUTDATED,

    @SerializedName("app_no_local")
    APP_NO_LOCAL,
}

// ============================================================================
// Hub Config (mirrors Rust HubItem / HubConfigGson)
// ============================================================================

data class HubConfigInfo(
    @SerializedName("hub_name") val hubName: String = "",
    @SerializedName("hub_icon_url") val hubIconUrl: String? = null,
)

data class HubConfig(
    @SerializedName("base_version") val baseVersion: Int = 0,
    @SerializedName("config_version") val configVersion: Int = 0,
    @SerializedName("uuid") val uuid: String = "",
    @SerializedName("info") val info: HubConfigInfo = HubConfigInfo(),
    @SerializedName("api_keywords") val apiKeywords: List<String> = emptyList(),
    @SerializedName("auth_keywords") val authKeywords: List<String> = emptyList(),
    @SerializedName("app_url_templates") val appUrlTemplates: List<String> = emptyList(),
    @SerializedName("target_check_api") val targetCheckApi: String? = null,
)

// ============================================================================
// App Config (mirrors Rust AppItem / AppConfigGson)
// ============================================================================

data class AppConfigInfo(
    @SerializedName("name") val name: String = "",
    @SerializedName("url") val url: String = "",
    @SerializedName("desc") val desc: String? = null,
    @SerializedName("extra_map") val extraMap: Map<String, String> = emptyMap(),
)

data class AppConfig(
    @SerializedName("base_version") val baseVersion: Int = 0,
    @SerializedName("config_version") val configVersion: Int = 0,
    @SerializedName("uuid") val uuid: String = "",
    @SerializedName("base_hub_uuid") val baseHubUuid: String = "",
    @SerializedName("info") val info: AppConfigInfo = AppConfigInfo(),
)

// ============================================================================
// Database Records (mirrors Rust models)
// ============================================================================

data class AppRecord(
    @SerializedName("id") val id: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("app_id") val appId: Map<String, String?> = emptyMap(),
    @SerializedName("invalid_version_number_field_regex") val invalidVersionNumberFieldRegex: String? = null,
    @SerializedName("include_version_number_field_regex") val includeVersionNumberFieldRegex: String? = null,
    @SerializedName("ignore_version_number") val ignoreVersionNumber: String? = null,
    @SerializedName("cloud_config") val cloudConfig: AppConfig? = null,
    @SerializedName("enable_hub_list") val enableHubList: String? = null,
    @SerializedName("star") val star: Boolean? = null,
)

data class HubRecord(
    @SerializedName("uuid") val uuid: String = "",
    @SerializedName("hub_config") val hubConfig: HubConfig = HubConfig(),
    @SerializedName("auth") val auth: Map<String, String> = emptyMap(),
    @SerializedName("ignore_app_id_list") val ignoreAppIdList: List<Map<String, String?>> = emptyList(),
    @SerializedName("applications_mode") val applicationsMode: Int = 0,
    @SerializedName("user_ignore_app_id_list") val userIgnoreAppIdList: List<Map<String, String?>> = emptyList(),
    @SerializedName("sort_point") val sortPoint: Int = 0,
)

data class ExtraHubRecord(
    @SerializedName("id") val id: String = "",
    @SerializedName("enable_global") val enableGlobal: Boolean = false,
    @SerializedName("url_replace_search") val urlReplaceSearch: String? = null,
    @SerializedName("url_replace_string") val urlReplaceString: String? = null,
)

data class ExtraAppRecord(
    @SerializedName("id") val id: String = "",
    @SerializedName("app_id") val appId: Map<String, String?> = emptyMap(),
    @SerializedName("mark_version_number") val markVersionNumber: String? = null,
)

// ============================================================================
// Manager Events (mirrors Rust ManagerEvent enum)
// ============================================================================

data class ManagerEvent(
    @SerializedName("type") val type: String = "",
    // AppStatusChanged fields
    @SerializedName("record_id") val recordId: String? = null,
    @SerializedName("old_status") val oldStatus: AppStatus? = null,
    @SerializedName("new_status") val newStatus: AppStatus? = null,
    // RenewProgress fields
    @SerializedName("done") val done: Int? = null,
    @SerializedName("total") val total: Int? = null,
    // AppAdded / AppDatabaseChanged fields
    @SerializedName("record") val record: AppRecord? = null,
)
