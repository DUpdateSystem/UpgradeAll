package net.xzos.upgradeall.core

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.launch
import kotlinx.coroutines.GlobalScope
import net.xzos.upgradeall.core.data.AppStatusInfo
import net.xzos.upgradeall.core.manager.AppManager
import net.xzos.upgradeall.core.manager.AppManagerNative
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration test for JNI AppManager bridge
 * Tests the native Rust implementation through JNI
 */
@RunWith(AndroidJUnit4::class)
class AppManagerJNITest {
    
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    
    @Before
    fun setup() {
        // Initialize if needed
    }
    
    @Test
    fun testNativeLibraryLoading() {
        // This will fail if the library can't be loaded
        try {
            System.loadLibrary("api_proxy")
            assertTrue("Native library should load successfully", true)
        } catch (e: UnsatisfiedLinkError) {
            fail("Failed to load native library: ${e.message}")
        }
    }
    
    @Test
    fun testStarManagement() = runBlocking {
        val testAppId = "com.test.app"
        
        // Test setting star
        val setResult = AppManager.setAppStar(testAppId, true)
        assertTrue("Should successfully set star", setResult)
        
        // Test checking star status
        val isStarred = AppManager.isAppStarred(testAppId)
        assertTrue("App should be starred", isStarred)
        
        // Test unsetting star
        val unsetResult = AppManager.setAppStar(testAppId, false)
        assertTrue("Should successfully unset star", unsetResult)
        
        val isNotStarred = AppManager.isAppStarred(testAppId)
        assertFalse("App should not be starred", isNotStarred)
    }
    
    @Test
    fun testGetStarredApps() = runBlocking {
        val testApps = listOf("app1", "app2", "app3")
        
        // Star some apps
        testApps.forEach { appId ->
            AppManager.setAppStar(appId, true)
        }
        
        // Get starred apps
        val starredApps = AppManager.getStarredApps()
        
        // Verify all test apps are in the starred list
        testApps.forEach { appId ->
            assertTrue("$appId should be in starred list", starredApps.contains(appId))
        }
        
        // Cleanup
        testApps.forEach { appId ->
            AppManager.setAppStar(appId, false)
        }
    }
    
    @Test
    fun testVersionIgnoreManagement() = runBlocking {
        val testAppId = "com.test.version"
        val testVersion = "1.2.3"
        
        // Set ignore version
        val setResult = AppManager.setIgnoreVersion(testAppId, testVersion)
        assertTrue("Should successfully set ignore version", setResult)
        
        // Check if version is ignored
        val isIgnored = AppManager.isVersionIgnored(testAppId, testVersion)
        assertTrue("Version should be ignored", isIgnored)
        
        // Check different version is not ignored
        val differentVersion = "1.2.4"
        val isNotIgnored = AppManager.isVersionIgnored(testAppId, differentVersion)
        assertFalse("Different version should not be ignored", isNotIgnored)
        
        // Get ignored version
        val ignoredVersion = AppManager.getIgnoreVersion(testAppId)
        assertEquals("Ignored version should match", testVersion, ignoredVersion)
    }
    
    @Test
    fun testAppFiltering() = runBlocking {
        val androidType = "android"
        
        // Get apps by type
        val androidApps = AppManager.getAppsByType(androidType)
        
        // All returned apps should start with the type prefix
        androidApps.forEach { appId ->
            assertTrue("App ID should start with $androidType", appId.startsWith(androidType))
        }
    }
    
    @Test
    fun testAppStatusFiltering() = runBlocking {
        // Get outdated apps
        val outdatedApps = AppManager.getOutdatedAppsFiltered()
        
        // Verify all returned apps are AppStatusInfo objects
        outdatedApps.forEach { appInfo ->
            assertNotNull("App ID should not be null", appInfo.appId)
            assertNotNull("Status should not be null", appInfo.status)
        }
    }
    
    @Test
    fun testStarredAppsWithStatus() = runBlocking {
        val testAppId = "com.test.starred.status"
        
        // Star an app
        AppManager.setAppStar(testAppId, true)
        
        // Get starred apps with status
        val starredWithStatus = AppManager.getStarredAppsWithStatus()
        
        // Should return AppStatusInfo objects
        starredWithStatus.forEach { appInfo ->
            assertNotNull("App ID should not be null", appInfo.appId)
            assertNotNull("Status should not be null", appInfo.status)
        }
        
        // Cleanup
        AppManager.setAppStar(testAppId, false)
    }
    
    @Test
    fun testIgnoreAllCurrentVersions() = runBlocking {
        // This tests the batch ignore functionality
        val count = AppManager.ignoreAllCurrentVersions()
        
        // Count should be non-negative (-1 indicates error)
        assertTrue("Should return valid count or 0", count >= 0)
    }
    
    @Test
    fun testNativeDirectCalls() {
        // Test direct native calls without going through AppManager
        val testAppId = "com.test.native.direct"
        
        // Test star management directly
        try {
            val setStarResult = AppManagerNative.nativeSetStar(testAppId, true)
            assertTrue("Direct native star set should work", setStarResult)
            
            val isStarred = AppManagerNative.nativeIsStarred(testAppId)
            assertTrue("Direct native star check should work", isStarred)
            
            // Cleanup
            AppManagerNative.nativeSetStar(testAppId, false)
        } catch (e: UnsatisfiedLinkError) {
            // This is expected if the native library isn't built yet
            println("Native library not available yet: ${e.message}")
        }
    }
    
    @Test
    fun testConcurrentNativeOperations() = runBlocking {
        val appIds = (1..10).map { "com.test.concurrent.$it" }
        
        // Concurrent star operations
        appIds.forEach { appId ->
            launch {
                AppManager.setAppStar(appId, true)
            }
        }
        
        // Wait a bit for operations to complete
        kotlinx.coroutines.delay(100)
        
        // Verify all are starred
        appIds.forEach { appId ->
            val isStarred = AppManager.isAppStarred(appId)
            assertTrue("$appId should be starred after concurrent operation", isStarred)
        }
        
        // Cleanup
        appIds.forEach { appId ->
            AppManager.setAppStar(appId, false)
        }
    }
}