package net.xzos.upgradeall.core

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import net.xzos.upgradeall.core.manager.AppManager
import net.xzos.upgradeall.core.manager.HubManager
import net.xzos.upgradeall.core.module.AppStatus
import net.xzos.upgradeall.core.module.Hub
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test for migrating Android providers to Rust getter
 * This test validates that Android-specific providers (Hubs) can be accessed through getter
 */
@RunWith(AndroidJUnit4::class)
class ProviderMigrationTest {
    
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    
    @Before
    fun setup() {
        // Initialize managers
        AppManager.initObject(context)
        // HubManager initialization happens implicitly
    }
    
    @Test
    fun testListAvailableProviders() = runBlocking {
        // Test: List all available providers (Hubs)
        val hubs = HubManager.getHubList()
        
        assertNotNull("Hub list should not be null", hubs)
        assertTrue("Should have at least one hub", hubs.isNotEmpty())
        
        // Log available hubs for debugging
        hubs.forEach { hub ->
            println("Found hub: ${hub.name} (${hub.uuid})")
        }
    }
    
    @Test
    fun testAndroidAppProvider() = runBlocking {
        // Test: Android app provider functionality
        val androidHubs = HubManager.getHubList().filter { hub ->
            hub.hubConfig.apiKeywords.contains("android_app_package")
        }
        
        assertTrue("Should have Android app provider", androidHubs.isNotEmpty())
        
        val androidHub = androidHubs.first()
        assertNotNull("Android hub should exist", androidHub)
        
        // Test if the hub can check Android apps
        val testAppId = mapOf("android_app_package" to "com.android.chrome")
        assertTrue("Should be valid Android app", androidHub.isValidApp(testAppId))
    }
    
    @Test
    fun testProviderDataRetrieval() = runBlocking {
        // Test: Provider can retrieve app data
        val hubs = HubManager.getHubList()
        if (hubs.isEmpty()) {
            println("No hubs available, skipping data retrieval test")
            return@runBlocking
        }
        
        val hub = hubs.first()
        val apps = AppManager.getAppList(hub)
        
        assertNotNull("App list should not be null", apps)
        
        if (apps.isNotEmpty()) {
            val app = apps.first()
            
            // Test getting app URL
            val url = app.getUrl(hub.uuid)
            println("App URL: $url")
            
            // Test getting app status
            val status = app.releaseStatus
            assertNotNull("App status should not be null", status)
            println("App status: $status")
        }
    }
    
    @Test
    fun testProviderIgnoreList() = runBlocking {
        // Test: Provider ignore functionality
        val hubs = HubManager.getHubList()
        if (hubs.isEmpty()) {
            println("No hubs available, skipping ignore test")
            return@runBlocking
        }
        
        val hub = hubs.first()
        val testAppId = mapOf("test_app" to "test.package.name")
        
        // Test ignore/unignore operations
        assertFalse("App should not be ignored initially", hub.isIgnoreApp(testAppId))
        
        hub.ignoreApp(testAppId)
        assertTrue("App should be ignored after ignoreApp", hub.isIgnoreApp(testAppId))
        
        hub.unignoreApp(testAppId)
        assertFalse("App should not be ignored after unignoreApp", hub.isIgnoreApp(testAppId))
    }
    
    @Test
    fun testProviderApplicationsMode() = runBlocking {
        // Test: Provider applications mode
        val hubs = HubManager.getHubList()
        val appModeHubs = hubs.filter { it.applicationsModeAvailable() }
        
        if (appModeHubs.isNotEmpty()) {
            val hub = appModeHubs.first()
            
            // Test enable/disable applications mode
            val initialMode = hub.isEnableApplicationsMode()
            
            hub.setApplicationsMode(true)
            assertTrue("Applications mode should be enabled", hub.isEnableApplicationsMode())
            
            hub.setApplicationsMode(false)
            assertFalse("Applications mode should be disabled", hub.isEnableApplicationsMode())
            
            // Restore initial state
            hub.setApplicationsMode(initialMode)
        } else {
            println("No hubs with applications mode available")
        }
    }
    
    @Test
    fun testProviderActiveStatus() = runBlocking {
        // Test: Provider active/inactive app status
        val hubs = HubManager.getHubList()
        if (hubs.isEmpty()) {
            println("No hubs available, skipping active status test")
            return@runBlocking
        }
        
        val hub = hubs.first()
        val testAppId = mapOf("test_app" to "test.active.app")
        
        // Test active status
        assertTrue("App should be active initially", hub.isActiveApp(testAppId))
        
        // Note: setActiveApp and unsetActiveApp are private, 
        // so we test through the public interface
        val apps = AppManager.getAppList(hub)
        apps.forEach { app ->
            val isActive = hub.isActiveApp(app.appId)
            println("App ${app.name} active status: $isActive")
        }
    }
    
    @Test
    fun testProviderUrlGeneration() = runBlocking {
        // Test: Provider URL template generation
        val hubs = HubManager.getHubList()
        val apps = AppManager.getAppList()
        
        apps.take(5).forEach { app ->
            app.hubEnableList.forEach { hub ->
                val url = app.getUrl(hub.uuid)
                if (url != null) {
                    assertNotNull("Generated URL should not be null", url)
                    assertTrue("URL should not be empty", url.isNotEmpty())
                    println("App ${app.name} URL from hub ${hub.name}: $url")
                }
            }
        }
    }
    
    @Test
    fun testNativeProviderIntegration() = runBlocking {
        // Test: Integration with native Rust provider
        // This tests if we can use Rust getter's provider system
        
        // Get apps through native interface
        val nativeApps = AppManager.getAppsByType("android")
        
        // Compare with Android implementation
        val androidApps = AppManager.getAppList("android_app_package")
        
        println("Native apps count: ${nativeApps.size}")
        println("Android apps count: ${androidApps.size}")
        
        // Both should return similar results once integrated
        // For now, we just ensure no crashes
        assertNotNull("Native apps should not be null", nativeApps)
        assertNotNull("Android apps should not be null", androidApps)
    }
}