package net.xzos.upgradeall.core.provider

import net.xzos.upgradeall.core.data.Release

/**
 * JNI wrapper for Rust Provider implementation
 * This bridges Android-specific providers to the Rust getter system
 */
object ProviderNative {
    init {
        try {
            System.loadLibrary("api_proxy")
        } catch (e: UnsatisfiedLinkError) {
            System.err.println("Warning: Native library api_proxy not loaded: ${e.message}")
        }
    }

    // ========== Provider Registration ==========
    
    @JvmStatic
    external fun nativeRegisterAndroidProvider(
        providerId: String,
        name: String,
        apiKeywords: Array<String>
    ): Boolean
    
    @JvmStatic
    external fun nativeRegisterMagiskProvider(
        providerId: String,
        name: String,
        repoUrl: String
    ): Boolean
    
    // ========== Provider Operations ==========
    
    @JvmStatic
    external fun nativeCheckApp(providerId: String, appId: String): Boolean
    
    @JvmStatic
    external fun nativeGetLatestRelease(
        providerId: String,
        appId: String,
        appType: String
    ): Release?
    
    // ========== JNI Callbacks ==========
    
    @JvmStatic
    external fun nativeSetAndroidCallback(
        providerId: String,
        callback: AndroidProviderCallback
    ): Boolean
    
    // ========== Provider List Operations ==========
    
    @JvmStatic
    external fun nativeListProviders(): Array<String>
    
    @JvmStatic
    external fun nativeGetProviderName(providerId: String): String
}

/**
 * Callback interface for Android-specific operations
 * Implemented in Kotlin and called from Rust through JNI
 */
interface AndroidProviderCallback {
    fun getInstalledVersion(packageName: String): String?
    fun getInstalledApps(): List<AndroidAppInfo>
    fun isAppInstalled(packageName: String): Boolean
    fun getAppInfo(packageName: String): AndroidAppInfo?
}

/**
 * Android app information
 */
data class AndroidAppInfo(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val versionCode: Int,
    val isSystemApp: Boolean
)