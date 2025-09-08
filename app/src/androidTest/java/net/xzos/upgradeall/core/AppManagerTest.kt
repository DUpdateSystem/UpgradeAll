package net.xzos.upgradeall.core

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import net.xzos.upgradeall.core.database.metaDatabase
import net.xzos.upgradeall.core.database.table.AppEntity
import net.xzos.upgradeall.core.manager.AppManager
import net.xzos.upgradeall.core.manager.UpdateStatus
import net.xzos.upgradeall.core.module.AppStatus
import net.xzos.upgradeall.core.module.app.App
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/**
 * Test suite for AppManager to ensure UI interface behavior remains consistent during migration
 */
@RunWith(AndroidJUnit4::class)
class AppManagerTest {
    
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var testApp: App
    private lateinit var testAppEntity: AppEntity
    
    @Before
    fun setup() {
        // Initialize AppManager
        AppManager.initObject(context)
        
        // Create test app entity
        testAppEntity = AppEntity(
            name = "TestApp_${UUID.randomUUID()}",
            appId = mapOf("test" to "com.test.app"),
            invalidVersionNumberFieldRegexString = "v([\\d.]+)",
            _enableHubUuidListString = "",
            startRaw = null
        )
    }
    
    @After
    fun tearDown() = runBlocking {
        // Clean up test data
        try {
            if (::testApp.isInitialized) {
                AppManager.removeApp(testApp)
            }
        } catch (e: Exception) {
            // Ignore cleanup errors
        }
    }
    
    @Test
    fun testAddAndRetrieveApp() = runBlocking {
        // Test adding an app
        val savedApp = AppManager.saveApp(testAppEntity)
        assertNotNull("App should be saved successfully", savedApp)
        testApp = savedApp!!
        
        // Test retrieving app by ID
        val retrievedApp = AppManager.getAppById(testAppEntity.appId)
        assertNotNull("Should find app by ID", retrievedApp)
        assertEquals("App names should match", testAppEntity.name, retrievedApp?.name)
    }
    
    @Test
    fun testGetAppList() {
        // Test getting all apps
        val allApps = AppManager.getAppList()
        assertNotNull("App list should not be null", allApps)
        assertTrue("App list should not be empty", allApps.isNotEmpty())
    }
    
    @Test
    fun testGetAppByStatus() = runBlocking {
        // Add test app
        val savedApp = AppManager.saveApp(testAppEntity)
        assertNotNull(savedApp)
        testApp = savedApp!!
        
        // Test getting apps by status
        val latestApps = AppManager.getAppList(AppStatus.APP_LATEST)
        assertNotNull("Latest apps list should not be null", latestApps)
        
        val outdatedApps = AppManager.getAppList(AppStatus.APP_OUTDATED)
        assertNotNull("Outdated apps list should not be null", outdatedApps)
    }
    
    @Test
    fun testAppUpdateNotifications() = runBlocking {
        var notificationReceived = false
        val observer: (App) -> Unit = { _ ->
            notificationReceived = true
        }
        
        AppManager.observe(UpdateStatus.APP_ADDED_NOTIFY, observer)
        
        // Add app and check notification
        val savedApp = AppManager.saveApp(testAppEntity)
        assertNotNull(savedApp)
        testApp = savedApp!!
        
        // Give some time for async notification
        Thread.sleep(100)
        
        assertTrue("Should receive app added notification", notificationReceived)
        
        AppManager.removeObserver(UpdateStatus.APP_ADDED_NOTIFY, observer)
    }
    
    @Test
    fun testGetAppByType() = runBlocking {
        // Create app with specific type
        val appEntity = AppEntity(
            name = "AndroidApp_${UUID.randomUUID()}",
            appId = mapOf("android" to "com.android.test"),
            invalidVersionNumberFieldRegexString = "v([\\d.]+)",
            _enableHubUuidListString = "",
            startRaw = null
        )
        
        val savedApp = AppManager.saveApp(appEntity)
        assertNotNull(savedApp)
        testApp = savedApp!!
        
        // Test getting apps by type
        val androidApps = AppManager.getAppList("android")
        assertNotNull("Android apps list should not be null", androidApps)
        assertTrue("Should find the test android app", 
            androidApps.any { it.appId["android"] == "com.android.test" })
    }
    
    @Test
    fun testAppStarStatus() = runBlocking {
        // Create starred app
        val starredEntity = testAppEntity.copy(startRaw = true)
        val savedApp = AppManager.saveApp(starredEntity)
        assertNotNull(savedApp)
        testApp = savedApp!!
        
        // Verify star status
        assertTrue("App should be starred", testApp.star)
        
        // Test filtering by star status
        val starredApps = AppManager.getAppList { it.star }
        assertTrue("Should find starred app", starredApps.contains(testApp))
    }
    
    @Test
    fun testRemoveApp() = runBlocking {
        // Add app
        val savedApp = AppManager.saveApp(testAppEntity)
        assertNotNull(savedApp)
        testApp = savedApp!!
        
        // Remove app
        AppManager.removeApp(testApp)
        
        // Verify app is removed
        val retrievedApp = AppManager.getAppById(testAppEntity.appId)
        assertNull("App should be removed", retrievedApp)
    }
}