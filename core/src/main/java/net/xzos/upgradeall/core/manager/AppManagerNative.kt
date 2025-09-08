package net.xzos.upgradeall.core.manager

import net.xzos.upgradeall.core.data.AppStatusInfo

/**
 * JNI wrapper for Rust AppManager implementation
 * This class provides native method declarations that are implemented in Rust
 */
object AppManagerNative {
    init {
        try {
            System.loadLibrary("api_proxy")
        } catch (e: UnsatisfiedLinkError) {
            // Library not available yet during testing
            System.err.println("Warning: Native library api_proxy not loaded: ${e.message}")
        }
    }

    // ========== Core AppManager Functions ==========
    
    @JvmStatic
    external fun nativeAddApp(appId: String, hubUuid: String): Boolean
    
    @JvmStatic
    external fun nativeRemoveApp(appId: String): Boolean
    
    @JvmStatic
    external fun nativeListApps(): Array<String>
    
    // ========== Star Management ==========
    
    @JvmStatic
    external fun nativeSetStar(appId: String, star: Boolean): Boolean
    
    @JvmStatic
    external fun nativeIsStarred(appId: String): Boolean
    
    @JvmStatic
    external fun nativeGetStarredApps(): Array<String>
    
    // ========== Version Ignore Management ==========
    
    @JvmStatic
    external fun nativeSetIgnoreVersion(appId: String, version: String): Boolean
    
    @JvmStatic
    external fun nativeGetIgnoreVersion(appId: String): String
    
    @JvmStatic
    external fun nativeIsVersionIgnored(appId: String, version: String): Boolean
    
    @JvmStatic
    external fun nativeIgnoreAllCurrentVersions(): Int
    
    // ========== App Filtering ==========
    
    @JvmStatic
    external fun nativeGetAppsByType(appType: String): Array<String>
    
    @JvmStatic
    external fun nativeGetAppsByStatus(status: String): Array<AppStatusInfo>
    
    @JvmStatic
    external fun nativeGetStarredAppsWithStatus(): Array<AppStatusInfo>
    
    @JvmStatic
    external fun nativeGetOutdatedAppsFiltered(): Array<AppStatusInfo>
}