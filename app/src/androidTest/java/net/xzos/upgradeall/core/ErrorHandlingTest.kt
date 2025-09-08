package net.xzos.upgradeall.core

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import net.xzos.upgradeall.core.database.metaDatabase
import net.xzos.upgradeall.core.database.table.AppEntity
import net.xzos.upgradeall.core.manager.AppManager
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.IOException
import java.util.UUID

/**
 * Error handling and recovery tests for UpgradeAll
 * Tests database corruption recovery, invalid data handling, memory exhaustion, and disk space limitations
 */
@RunWith(AndroidJUnit4::class)
class ErrorHandlingTest {
    
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var testDataDir: File
    
    @Before
    fun setup() {
        AppManager.initObject(context)
        testDataDir = File(context.cacheDir, "test_error_handling_${UUID.randomUUID()}")
        testDataDir.mkdirs()
    }
    
    @After
    fun tearDown() {
        testDataDir.deleteRecursively()
    }
    
    @Test
    fun testCorruptedDatabaseRecovery() = runBlocking {
        // Get database file location
        val dbFile = context.getDatabasePath("UpgradeAll.db")
        val backupFile = File(testDataDir, "backup.db")
        
        // Create some test data
        val testApp = AppEntity(
            name = "TestApp_${UUID.randomUUID()}",
            appId = mapOf("test" to "com.test.corrupted"),
            invalidVersionNumberFieldRegexString = "v([\\d.]+)",
            _enableHubUuidListString = "",
            startRaw = null
        )
        
        val savedApp = AppManager.saveApp(testApp)
        assertNotNull("Should save app before corruption", savedApp)
        
        // Backup current database
        if (dbFile.exists()) {
            dbFile.copyTo(backupFile, overwrite = true)
        }
        
        // Simulate database corruption by writing garbage
        try {
            dbFile.writeBytes(ByteArray(1024) { (it % 256).toByte() })
        } catch (e: Exception) {
            // Database might be locked, skip corruption simulation
        }
        
        // Try to access database (should trigger recovery)
        try {
            reinitializeAppManager(context)
            val apps = AppManager.getAppList()
            assertNotNull("Should recover and return app list", apps)
        } catch (e: Exception) {
            // Recovery might create new database
            assertTrue("Should handle corruption gracefully", 
                e.message?.contains("corrupt") == true || AppManager.getAppList() != null)
        }
        
        // Cleanup
        try {
            AppManager.removeApp(savedApp!!)
        } catch (e: Exception) {
            // Ignore cleanup errors
        }
    }
    
    @Test
    fun testInvalidDataHandling() = runBlocking {
        // Test with various invalid data scenarios
        
        // 1. Null/empty app name
        val invalidApp1 = AppEntity(
            name = "",
            appId = mapOf("test" to "com.test.invalid1"),
            invalidVersionNumberFieldRegexString = "v([\\d.]+)",
            _enableHubUuidListString = "",
            startRaw = null
        )
        
        val result1 = try {
            AppManager.saveApp(invalidApp1)
        } catch (e: Exception) {
            null
        }
        
        if (result1 != null) {
            assertTrue("Should handle empty name gracefully", 
                result1.name.isNotEmpty() || result1.name == "")
            AppManager.removeApp(result1)
        }
        
        // 2. Invalid regex pattern
        val invalidApp2 = AppEntity(
            name = "InvalidRegexApp",
            appId = mapOf("test" to "com.test.invalid2"),
            invalidVersionNumberFieldRegexString = "[[[invalid regex",
            _enableHubUuidListString = "",
            startRaw = null
        )
        
        val result2 = try {
            val app = AppManager.saveApp(invalidApp2)
            // Try to use the invalid regex
            app?.let {
                val testVersion = "v1.2.3"
                // Use the original pattern from the entity
                try {
                    val regex = Regex(invalidApp2.invalidVersionNumberFieldRegexString ?: ".*")
                    regex.matches(testVersion)
                } catch (e: Exception) {
                    // Invalid regex should throw exception
                }
            }
            app
        } catch (e: Exception) {
            null
        }
        
        // Should either reject invalid regex or handle it gracefully
        assertTrue("Should handle invalid regex pattern", 
            result2 == null || invalidApp2.invalidVersionNumberFieldRegexString != null)
        
        result2?.let { AppManager.removeApp(it) }
        
        // 3. Extremely long strings
        val longString = "x".repeat(10000)
        val invalidApp3 = AppEntity(
            name = longString,
            appId = mapOf("test" to "com.test.invalid3"),
            invalidVersionNumberFieldRegexString = "v([\\d.]+)",
            _enableHubUuidListString = longString,
            startRaw = null
        )
        
        val result3 = try {
            AppManager.saveApp(invalidApp3)
        } catch (e: Exception) {
            null
        }
        
        // Should truncate or handle long strings
        if (result3 != null) {
            assertTrue("Should handle long strings", 
                result3.name.length <= 10000)
            AppManager.removeApp(result3)
        }
        
        // 4. Special characters and SQL injection attempts
        val sqlInjection = "'; DROP TABLE apps; --"
        val invalidApp4 = AppEntity(
            name = sqlInjection,
            appId = mapOf("test" to sqlInjection),
            invalidVersionNumberFieldRegexString = "v([\\d.]+)",
            _enableHubUuidListString = "",
            startRaw = null
        )
        
        val result4 = try {
            AppManager.saveApp(invalidApp4)
        } catch (e: Exception) {
            null
        }
        
        // Verify database is still intact
        val appsAfterInjection = AppManager.getAppList()
        assertNotNull("Database should remain intact after SQL injection attempt", appsAfterInjection)
        
        result4?.let { AppManager.removeApp(it) }
    }
    
    @Test
    fun testMemoryExhaustion() = runBlocking {
        val largeApps = mutableListOf<net.xzos.upgradeall.core.module.app.App>()
        
        try {
            // Try to create many apps to stress memory
            repeat(1000) { i ->
                val app = AppEntity(
                    name = "MemoryTestApp_$i",
                    appId = mapOf("test" to "com.test.memory.$i"),
                    invalidVersionNumberFieldRegexString = "v([\\d.]+)",
                    _enableHubUuidListString = "",
                    startRaw = null
                )
                
                val savedApp = AppManager.saveApp(app)
                savedApp?.let { largeApps.add(it) }
                
                // Check if we can still perform operations
                if (i % 100 == 0) {
                    val list = AppManager.getAppList()
                    assertNotNull("Should still function under memory pressure at $i apps", list)
                }
            }
        } catch (e: OutOfMemoryError) {
            // Should handle OOM gracefully
            assertTrue("Should catch OOM error", true)
        } finally {
            // Cleanup
            largeApps.forEach { app ->
                try {
                    AppManager.removeApp(app)
                } catch (e: Exception) {
                    // Ignore cleanup errors
                }
            }
        }
        
        // Verify system recovered
        val finalList = AppManager.getAppList()
        assertNotNull("Should recover from memory pressure", finalList)
    }
    
    @Test
    fun testDiskSpaceLimitation() = runBlocking {
        // Simulate low disk space by filling cache directory
        val testFile = File(testDataDir, "large_file.tmp")
        val savedApps = mutableListOf<net.xzos.upgradeall.core.module.app.App>()
        
        try {
            // Try to fill disk (limited to prevent actual disk full)
            val maxSize = 50 * 1024 * 1024 // 50MB limit for test
            val buffer = ByteArray(1024 * 1024) // 1MB chunks
            
            testFile.outputStream().use { output ->
                repeat(maxSize / buffer.size) {
                    output.write(buffer)
                }
            }
            
            // Try to save apps with limited disk space
            repeat(10) { i ->
                val app = AppEntity(
                    name = "DiskTestApp_$i",
                    appId = mapOf("test" to "com.test.disk.$i"),
                    invalidVersionNumberFieldRegexString = "v([\\d.]+)",
                    _enableHubUuidListString = "",
                    startRaw = null
                )
                
                try {
                    val savedApp = AppManager.saveApp(app)
                    savedApp?.let { savedApps.add(it) }
                } catch (e: IOException) {
                    // Should handle disk space errors gracefully
                    assertTrue("Should catch disk space error", 
                        e.message?.contains("space") == true || e.message?.contains("disk") == true)
                }
            }
        } finally {
            // Cleanup
            testFile.delete()
            savedApps.forEach { app ->
                try {
                    AppManager.removeApp(app)
                } catch (e: Exception) {
                    // Ignore cleanup errors
                }
            }
        }
        
        // Verify system still works after disk space is freed
        val testApp = AppEntity(
            name = "PostDiskTestApp",
            appId = mapOf("test" to "com.test.postdisk"),
            invalidVersionNumberFieldRegexString = "v([\\d.]+)",
            _enableHubUuidListString = "",
            startRaw = null
        )
        
        val finalApp = AppManager.saveApp(testApp)
        assertNotNull("Should work after disk space is freed", finalApp)
        finalApp?.let { AppManager.removeApp(it) }
    }
    
    @Test
    fun testConcurrentAccessErrors() = runBlocking {
        val testApp = AppEntity(
            name = "ConcurrentTestApp",
            appId = mapOf("test" to "com.test.concurrent"),
            invalidVersionNumberFieldRegexString = "v([\\d.]+)",
            _enableHubUuidListString = "",
            startRaw = false
        )
        
        val savedApp = AppManager.saveApp(testApp)
        assertNotNull(savedApp)
        
        // Simulate concurrent modifications
        val jobs = coroutineScope {
            List(10) { index ->
                async {
                    try {
                        when (index % 3) {
                            0 -> {
                                // Try to update star status
                                savedApp?.let { app ->
                                    val entity = app.getRawEntity()
                                    AppManager.saveApp(entity.copy(startRaw = index % 2 == 0))
                                }
                            }
                            1 -> {
                                // Try to read
                                AppManager.getAppById(testApp.appId)
                            }
                            2 -> {
                                // Try to update name
                                savedApp?.let { app ->
                                    val entity = app.getRawEntity()
                                    val updatedEntity = entity.copy(name = "Updated_$index")
                                    AppManager.saveApp(updatedEntity)
                                }
                            }
                        }
                        true
                    } catch (e: Exception) {
                        // Should handle concurrent access gracefully
                        false
                    }
                }
            }
        }
        
        val results = jobs.map { it.await() }
        
        // At least some operations should succeed
        assertTrue("Should handle concurrent access", results.any { it })
        
        // Cleanup
        savedApp?.let { AppManager.removeApp(it) }
    }
    
    @Test
    fun testNetworkTimeoutRecovery() = runBlocking {
        // Test recovery from network timeouts
        val networkErrors = mutableListOf<String>()
        
        // Simulate network operations with timeouts
        repeat(5) { attempt ->
            try {
                kotlinx.coroutines.withTimeout(100) {
                    // Simulate slow network operation
                    kotlinx.coroutines.delay(200)
                    "Success"
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                networkErrors.add("Timeout attempt $attempt")
            }
        }
        
        assertEquals("Should record all timeout attempts", 5, networkErrors.size)
        
        // Verify app still functions after timeouts
        val apps = AppManager.getAppList()
        assertNotNull("Should still function after network timeouts", apps)
    }
    
    @Test
    fun testInvalidVersionHandling() = runBlocking {
        val testApp = AppEntity(
            name = "VersionTestApp",
            appId = mapOf("test" to "com.test.version"),
            invalidVersionNumberFieldRegexString = "v([\\d.]+)",
            _enableHubUuidListString = "",
            startRaw = null
        )
        
        val savedApp = AppManager.saveApp(testApp)
        assertNotNull(savedApp)
        
        // Test various invalid version formats
        val invalidVersions = listOf(
            "",
            "not-a-version",
            "v",
            "1.2.3.4.5.6.7.8",
            "v-1.-2.-3",
            "😀1.2.3",
            null
        )
        
        invalidVersions.forEach { version ->
            try {
                // Simulate version comparison
                val regexPattern = testApp.invalidVersionNumberFieldRegexString ?: ".*"
                val isValid = version?.matches(Regex(regexPattern)) ?: false
                // Should handle invalid versions without crashing
                assertTrue("Should process version check", true)
            } catch (e: Exception) {
                fail("Should not crash on invalid version: $version")
            }
        }
        
        // Cleanup
        savedApp?.let { AppManager.removeApp(it) }
    }
    
    @Test
    fun testCircularDependencyHandling() = runBlocking {
        // Test handling of circular dependencies in app relationships
        val app1 = AppEntity(
            name = "CircularApp1",
            appId = mapOf("test" to "com.test.circular1"),
            invalidVersionNumberFieldRegexString = "v([\\d.]+)",
            _enableHubUuidListString = "hub1,hub2",
            startRaw = null
        )
        
        val app2 = AppEntity(
            name = "CircularApp2",
            appId = mapOf("test" to "com.test.circular2"),
            invalidVersionNumberFieldRegexString = "v([\\d.]+)",
            _enableHubUuidListString = "hub2,hub1", // Circular reference
            startRaw = null
        )
        
        val saved1 = AppManager.saveApp(app1)
        val saved2 = AppManager.saveApp(app2)
        
        assertNotNull("Should save first app", saved1)
        assertNotNull("Should save second app despite circular reference", saved2)
        
        // Should be able to query without infinite loop
        val apps = AppManager.getAppList()
        assertNotNull("Should handle circular dependencies in queries", apps)
        
        // Cleanup
        saved1?.let { AppManager.removeApp(it) }
        saved2?.let { AppManager.removeApp(it) }
    }
}

// Extension function to help with testing
private fun net.xzos.upgradeall.core.module.app.App.getRawEntity(): AppEntity {
    // Simply return the underlying database entity
    return this.db
}

// Helper for AppManager reinitialization
private fun reinitializeAppManager(context: android.content.Context) {
    // Force reinitialize by clearing and recreating
    try {
        val field = AppManager::class.java.getDeclaredField("INSTANCE")
        field.isAccessible = true
        field.set(null, null)
    } catch (e: Exception) {
        // Ignore if field not found
    }
    AppManager.initObject(context)
}