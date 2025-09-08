package net.xzos.upgradeall.core.provider

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.xzos.upgradeall.core.database.table.HubEntity
import net.xzos.upgradeall.core.module.Hub

/**
 * Bridge between Android Hub system and Rust Provider system
 * Gradually migrates functionality from Hub to Provider
 */
class ProviderBridge(private val context: Context) {
    
    private val packageManager = context.packageManager
    private val registeredProviders = mutableSetOf<String>()
    
    /**
     * Register a Hub as a Provider in the Rust system
     */
    suspend fun registerHubAsProvider(hub: Hub): Boolean = withContext(Dispatchers.IO) {
        val providerId = hub.uuid
        val name = hub.name
        val apiKeywords = hub.hubConfig.apiKeywords.toTypedArray()
        
        // Check if already registered
        if (registeredProviders.contains(providerId)) {
            return@withContext true
        }
        
        // Register based on hub type
        val registered = when {
            apiKeywords.contains("android_app_package") -> {
                // Register as Android app provider
                ProviderNative.nativeRegisterAndroidProvider(providerId, name, apiKeywords) &&
                setupAndroidCallback(providerId)
            }
            apiKeywords.contains("android_magisk_module") -> {
                // Register as Magisk module provider
                val repoUrl = hub.hubConfig.targetCheckApi ?: ""
                ProviderNative.nativeRegisterMagiskProvider(providerId, name, repoUrl) &&
                setupAndroidCallback(providerId)
            }
            else -> {
                // Register as generic provider (to be implemented)
                false
            }
        }
        
        if (registered) {
            registeredProviders.add(providerId)
        }
        
        registered
    }
    
    /**
     * Setup Android-specific callbacks for the provider
     */
    private fun setupAndroidCallback(providerId: String): Boolean {
        val callback = object : AndroidProviderCallback {
            override fun getInstalledVersion(packageName: String): String? {
                return try {
                    val packageInfo = packageManager.getPackageInfo(packageName, 0)
                    packageInfo.versionName
                } catch (e: PackageManager.NameNotFoundException) {
                    null
                }
            }
            
            override fun getInstalledApps(): List<AndroidAppInfo> {
                return packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                    .map { appInfo ->
                        try {
                            val packageInfo = packageManager.getPackageInfo(appInfo.packageName, 0)
                            AndroidAppInfo(
                                packageName = appInfo.packageName,
                                appName = packageManager.getApplicationLabel(appInfo).toString(),
                                versionName = packageInfo.versionName ?: "",
                                versionCode = packageInfo.versionCode,
                                isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    .filterNotNull()
            }
            
            override fun isAppInstalled(packageName: String): Boolean {
                return try {
                    packageManager.getPackageInfo(packageName, 0)
                    true
                } catch (e: PackageManager.NameNotFoundException) {
                    false
                }
            }
            
            override fun getAppInfo(packageName: String): AndroidAppInfo? {
                return try {
                    val appInfo = packageManager.getApplicationInfo(packageName, 0)
                    val packageInfo = packageManager.getPackageInfo(packageName, 0)
                    AndroidAppInfo(
                        packageName = packageName,
                        appName = packageManager.getApplicationLabel(appInfo).toString(),
                        versionName = packageInfo.versionName ?: "",
                        versionCode = packageInfo.versionCode,
                        isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    )
                } catch (e: PackageManager.NameNotFoundException) {
                    null
                }
            }
        }
        
        return ProviderNative.nativeSetAndroidCallback(providerId, callback)
    }
    
    /**
     * Check if an app is available through a provider
     */
    suspend fun checkAppWithProvider(providerId: String, appId: String): Boolean = 
        withContext(Dispatchers.IO) {
            ProviderNative.nativeCheckApp(providerId, appId)
        }
    
    /**
     * Get latest release from provider
     */
    suspend fun getLatestReleaseFromProvider(
        providerId: String,
        appId: String,
        appType: String = "android_app_package"
    ): Any? = withContext(Dispatchers.IO) {
        ProviderNative.nativeGetLatestRelease(providerId, appId, appType)
    }
    
    /**
     * List all registered providers
     */
    fun listProviders(): List<String> {
        return ProviderNative.nativeListProviders().toList()
    }
    
    /**
     * Get provider name
     */
    fun getProviderName(providerId: String): String {
        return ProviderNative.nativeGetProviderName(providerId)
    }
    
    /**
     * Migrate all hubs to providers
     */
    suspend fun migrateAllHubsToProviders(hubs: List<Hub>): Int = withContext(Dispatchers.IO) {
        var successCount = 0
        hubs.forEach { hub ->
            if (registerHubAsProvider(hub)) {
                successCount++
            }
        }
        successCount
    }
    
    companion object {
        @Volatile
        private var INSTANCE: ProviderBridge? = null
        
        fun getInstance(context: Context): ProviderBridge {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ProviderBridge(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}