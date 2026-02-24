package net.xzos.upgradeall.core.manager

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.xzos.upgradeall.core.coreConfig
import net.xzos.upgradeall.core.utils.AutoTemplate
import net.xzos.upgradeall.core.utils.log.Log
import net.xzos.upgradeall.core.utils.log.ObjectTag
import net.xzos.upgradeall.core.utils.log.ObjectTag.Companion.core
import net.xzos.upgradeall.core.websdk.getterPort
import net.xzos.upgradeall.getter.rpc.AppConfig
import net.xzos.upgradeall.getter.rpc.HubConfig

/**
 * Cloud config manager — now a thin RPC proxy over the Rust CloudConfigGetter.
 *
 * All heavy logic (fetching, parsing, applying configs, solving hub dependencies,
 * bulk renewal) runs inside Rust. Kotlin is only responsible for:
 *  - supplying the API URL from coreConfig
 *  - forwarding calls to the Rust getter via GetterService RPC
 *  - caching the last-known app/hub list in memory for synchronous lookups
 *  - mapping the Boolean RPC result back to GetStatus for legacy UI callbacks
 */
object CloudConfigGetter {
    private const val TAG = "CloudConfigGetter"
    private val logObjectTag = ObjectTag(core, TAG)

    private val renewMutex = Mutex()

    /** In-memory cache populated by renew(). */
    @Volatile private var cachedAppList: List<AppConfig> = emptyList()
    @Volatile private var cachedHubList: List<HubConfig> = emptyList()

    private val apiUrl: String
        get() {
            val custom = coreConfig.cloud_rules_hub_url
            return if (custom.isNullOrBlank())
                "http://${coreConfig.update_server_url}/v1/rules/download/dev"
            else custom
        }

    private fun service() = getterPort.getService()

    // =========================================================================
    // Public API (mirrors the old CloudConfigGetter interface)
    // =========================================================================

    /**
     * Download and cache the latest cloud config list from Rust.
     * Initialises the Rust CloudConfigGetter with the current API URL first.
     */
    suspend fun renew() {
        renewMutex.withLock {
            try {
                service().cloudConfigInit(apiUrl)
                service().cloudConfigRenew()
                // Refresh in-memory caches
                cachedAppList = service().cloudConfigGetAppList()
                cachedHubList = service().cloudConfigGetHubList()
                Log.d(logObjectTag, TAG, "renew: ${cachedAppList.size} apps, ${cachedHubList.size} hubs")
            } catch (e: Exception) {
                Log.e(logObjectTag, TAG, "renew failed: $e")
            }
        }
    }

    val appConfigList: List<AppConfig>? get() = cachedAppList.ifEmpty { null }
    val hubConfigList: List<HubConfig>? get() = cachedHubList.ifEmpty { null }

    fun getAppCloudConfig(appUuid: String?): AppConfig? =
        cachedAppList.firstOrNull { it.uuid == appUuid }

    fun getHubCloudConfig(hubUuid: String?): HubConfig? =
        cachedHubList.firstOrNull { it.uuid == hubUuid }

    /**
     * Download and apply a cloud hub config by UUID.
     * Returns true on success.
     */
    suspend fun downloadCloudHubConfig(
        hubUuid: String?,
        notifyFun: (GetStatus) -> Unit,
    ): Boolean {
        if (hubUuid == null) {
            notifyFun(GetStatus.FAILED_GET_HUB_DATA)
            notifyFun(GetStatus.FAILED)
            return false
        }
        return try {
            notifyFun(GetStatus.SUCCESS_GET_HUB_DATA)
            val ok = service().cloudConfigApplyHub(hubUuid)
            if (ok) {
                notifyFun(GetStatus.SUCCESS_SAVE_HUB_DATA)
                notifyFun(GetStatus.SUCCESS)
                // Recheck apps that may have become invalid after hub config change.
                service().managerCheckInvalidApplications()
            } else {
                notifyFun(GetStatus.FAILED_SAVE_HUB_DATA)
                notifyFun(GetStatus.FAILED)
            }
            ok
        } catch (e: Exception) {
            Log.e(logObjectTag, TAG, "downloadCloudHubConfig failed: $e")
            notifyFun(GetStatus.FAILED_GET_HUB_DATA)
            notifyFun(GetStatus.FAILED)
            false
        }
    }

    /**
     * Download and apply a cloud app config by UUID (including hub dependency).
     * Returns true on success.
     */
    suspend fun downloadCloudAppConfig(
        appUuid: String?,
        notifyFun: (GetStatus) -> Unit,
    ): Boolean {
        if (appUuid == null) {
            notifyFun(GetStatus.FAILED_GET_APP_DATA)
            notifyFun(GetStatus.FAILED)
            return false
        }
        return try {
            notifyFun(GetStatus.SUCCESS_GET_APP_DATA)
            val ok = service().cloudConfigApplyApp(appUuid)
            if (ok) {
                notifyFun(GetStatus.SUCCESS_SAVE_APP_DATA)
                notifyFun(GetStatus.SUCCESS)
            } else {
                notifyFun(GetStatus.FAILED_SAVE_APP_DATA)
                notifyFun(GetStatus.FAILED)
            }
            ok
        } catch (e: Exception) {
            Log.e(logObjectTag, TAG, "downloadCloudAppConfig failed: $e")
            notifyFun(GetStatus.FAILED_GET_APP_DATA)
            notifyFun(GetStatus.FAILED)
            false
        }
    }

    /** Bulk-update all installed apps and hubs whose cloud config version has increased. */
    suspend fun renewAllFromCloud() {
        try {
            service().cloudConfigRenewAll()
        } catch (e: Exception) {
            Log.e(logObjectTag, TAG, "renewAllFromCloud failed: $e")
        }
    }
}

/**
 * Compute the app_id map for a cloud AppConfig.
 *
 * Mirrors the old AppConfigGson.getAppId() logic:
 * 1. Extract from info.url using the hub's appUrlTemplates.
 * 2. Merge info.extraMap (extra_map wins on key conflicts).
 */
fun AppConfig.getAppId(): Map<String, String>? {
    val hubConfig = CloudConfigGetter.getHubCloudConfig(baseHubUuid) ?: return null
    val fromUrl = AutoTemplate.urlToAppId(info.url, hubConfig.appUrlTemplates) ?: emptyMap()
    return fromUrl + info.extraMap
}

enum class GetStatus(val value: Int) {
    SUCCESS(1),
    SUCCESS_GET_APP_DATA(2),
    SUCCESS_GET_HUB_DATA(3),
    SUCCESS_SAVE_APP_DATA(4),
    SUCCESS_SAVE_HUB_DATA(5),

    FAILED(-1),
    FAILED_GET_APP_DATA(-2),
    FAILED_GET_HUB_DATA(-3),
    FAILED_SAVE_APP_DATA(-4),
    FAILED_SAVE_HUB_DATA(-5);
}
