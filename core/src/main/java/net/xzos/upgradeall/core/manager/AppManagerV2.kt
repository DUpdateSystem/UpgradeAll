package net.xzos.upgradeall.core.manager

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.xzos.upgradeall.core.data.AppStatusInfo
import net.xzos.upgradeall.core.database.table.AppEntity
import net.xzos.upgradeall.core.module.AppStatus
import net.xzos.upgradeall.core.module.Hub
import net.xzos.upgradeall.core.module.app.App
import net.xzos.upgradeall.core.provider.ProviderBridge

/**
 * AppManagerV2 - Migrated version using Rust getter
 * This gradually replaces the old AppManager implementation
 */
object AppManagerV2 {
    
    private lateinit var providerBridge: ProviderBridge
    private var initialized = false
    
    /**
     * Initialize the manager with context
     */
    suspend fun initialize(context: Context) = withContext(Dispatchers.IO) {
        if (!initialized) {
            providerBridge = ProviderBridge.getInstance(context)
            
            // Initialize native AppManager
            AppManagerNative // This triggers the static init block
            
            // Migrate existing hubs to providers
            val hubs = HubManager.getHubList()
            val migrated = providerBridge.migrateAllHubsToProviders(hubs)
            println("Migrated $migrated hubs to providers")
            
            initialized = true
        }
    }
    
    // ========== Core Functions (Using Native) ==========
    
    /**
     * Add an app using native implementation
     */
    suspend fun addApp(appId: String, hubUuid: String): Boolean = withContext(Dispatchers.IO) {
        AppManagerNative.nativeAddApp(appId, hubUuid)
    }
    
    /**
     * Remove an app using native implementation
     */
    suspend fun removeApp(appId: String): Boolean = withContext(Dispatchers.IO) {
        AppManagerNative.nativeRemoveApp(appId)
    }
    
    /**
     * List all apps using native implementation
     */
    suspend fun listApps(): List<String> = withContext(Dispatchers.IO) {
        AppManagerNative.nativeListApps().toList()
    }
    
    // ========== Star Management (Using Native) ==========
    
    /**
     * Set star status for an app
     */
    suspend fun setAppStar(appId: String, star: Boolean): Boolean = withContext(Dispatchers.IO) {
        AppManagerNative.nativeSetStar(appId, star)
    }
    
    /**
     * Check if an app is starred
     */
    suspend fun isAppStarred(appId: String): Boolean = withContext(Dispatchers.IO) {
        AppManagerNative.nativeIsStarred(appId)
    }
    
    /**
     * Get all starred apps
     */
    suspend fun getStarredApps(): List<String> = withContext(Dispatchers.IO) {
        AppManagerNative.nativeGetStarredApps().toList()
    }
    
    // ========== Version Ignore Management (Using Native) ==========
    
    /**
     * Set ignored version for an app
     */
    suspend fun setIgnoreVersion(appId: String, version: String): Boolean = withContext(Dispatchers.IO) {
        AppManagerNative.nativeSetIgnoreVersion(appId, version)
    }
    
    /**
     * Get ignored version for an app
     */
    suspend fun getIgnoreVersion(appId: String): String? = withContext(Dispatchers.IO) {
        val version = AppManagerNative.nativeGetIgnoreVersion(appId)
        if (version.isEmpty()) null else version
    }
    
    /**
     * Check if a version is ignored
     */
    suspend fun isVersionIgnored(appId: String, version: String): Boolean = withContext(Dispatchers.IO) {
        AppManagerNative.nativeIsVersionIgnored(appId, version)
    }
    
    // ========== App Filtering (Using Native) ==========
    
    /**
     * Get apps by type
     */
    suspend fun getAppsByType(appType: String): List<String> = withContext(Dispatchers.IO) {
        AppManagerNative.nativeGetAppsByType(appType).toList()
    }
    
    /**
     * Get apps by status
     */
    suspend fun getAppsByStatus(status: AppStatus): List<AppStatusInfo> = withContext(Dispatchers.IO) {
        val statusString = when (status) {
            AppStatus.APP_PENDING -> AppStatusInfo.STATUS_PENDING
            AppStatus.APP_INACTIVE -> AppStatusInfo.STATUS_INACTIVE
            AppStatus.APP_LATEST -> AppStatusInfo.STATUS_LATEST
            AppStatus.APP_OUTDATED -> AppStatusInfo.STATUS_OUTDATED
            else -> AppStatusInfo.STATUS_PENDING
        }
        AppManagerNative.nativeGetAppsByStatus(statusString).toList()
    }
    
    /**
     * Get starred apps with their status
     */
    suspend fun getStarredAppsWithStatus(): List<AppStatusInfo> = withContext(Dispatchers.IO) {
        AppManagerNative.nativeGetStarredAppsWithStatus().toList()
    }
    
    /**
     * Get outdated apps excluding ignored versions
     */
    suspend fun getOutdatedAppsFiltered(): List<AppStatusInfo> = withContext(Dispatchers.IO) {
        AppManagerNative.nativeGetOutdatedAppsFiltered().toList()
    }
    
    // ========== Provider Integration ==========
    
    /**
     * Check app through provider
     */
    suspend fun checkAppWithProvider(hub: Hub, appId: String): Boolean = withContext(Dispatchers.IO) {
        providerBridge.checkAppWithProvider(hub.uuid, appId)
    }
    
    /**
     * Get latest release from provider
     */
    suspend fun getLatestReleaseFromProvider(hub: Hub, appId: String): Any? = withContext(Dispatchers.IO) {
        providerBridge.getLatestReleaseFromProvider(hub.uuid, appId)
    }
    
    /**
     * List all providers
     */
    fun listProviders(): List<String> {
        return providerBridge.listProviders()
    }
    
    // ========== Compatibility Layer ==========
    
    /**
     * Save app (compatibility with old interface)
     */
    suspend fun saveApp(appEntity: AppEntity): App? {
        // Convert to native format and save
        val appId = appEntity.appId.entries.firstOrNull()?.value ?: return null
        val hubUuid = appEntity.getSortHubUuidList().firstOrNull() ?: return null
        
        return if (addApp(appId, hubUuid)) {
            // Set star status if needed
            if (appEntity.star) {
                setAppStar(appId, true)
            }
            
            // Set ignored version if present
            appEntity.ignoreVersionNumber?.let { version ->
                setIgnoreVersion(appId, version)
            }
            
            // Return app object for compatibility
            App(appEntity)
        } else {
            null
        }
    }
    
    /**
     * Remove app (compatibility with old interface)
     */
    suspend fun removeApp(app: App): Boolean {
        val appId = app.appId.entries.firstOrNull()?.value ?: return false
        return removeApp(appId)
    }
    
    /**
     * Get app list (compatibility with old interface)
     */
    suspend fun getAppList(): List<App> {
        // This would need to be implemented to convert from native format
        // For now, return empty list
        return emptyList()
    }
    
    /**
     * Get app list by hub (compatibility with old interface)
     */
    suspend fun getAppList(hub: Hub): List<App> {
        // Use provider to get apps for this hub
        // For now, return empty list
        return emptyList()
    }
    
    /**
     * Get app list by status (compatibility with old interface)
     */
    suspend fun getAppList(status: AppStatus): List<App> {
        val statusInfos = getAppsByStatus(status)
        // Convert AppStatusInfo to App objects
        // This would need proper implementation
        return emptyList()
    }
}