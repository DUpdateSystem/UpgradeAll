package net.xzos.upgradeall.database

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import net.xzos.upgradeall.core.database.MetaDatabase
import net.xzos.upgradeall.core.database.table.AppEntity
import net.xzos.upgradeall.core.database.table.HubEntity
import net.xzos.upgradeall.core.utils.coroutines.coroutinesMutableListOf
import net.xzos.upgradeall.websdk.data.json.HubConfigGson
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.util.UUID

/**
 * Test suite for database migrations to configuration files
 * Ensures data integrity during SQL to config file migration
 */
@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {
    
    private lateinit var database: MetaDatabase
    private val TEST_DB = "migration-test"
    
    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        MetaDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )
    
    @Before
    fun createDb() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(
            context,
            MetaDatabase::class.java
        ).allowMainThreadQueries().build()
    }
    
    @After
    fun closeDb() {
        database.close()
    }
    
    @Test
    @Throws(IOException::class)
    fun testAppEntityDataIntegrity() = runBlocking {
        // Create test app entities with various configurations
        val testApps = listOf(
            AppEntity(
                name = "App1",
                appId = mapOf("android" to "com.test.app1"),
                invalidVersionNumberFieldRegexString = "v([\\d.]+)",
                cloudConfig = null,
                _enableHubUuidListString = "hub1 hub2",
                startRaw = true
            ),
            AppEntity(
                name = "App2",
                appId = mapOf(
                    "android" to "com.test.app2",
                    "package" to "app2-package"
                ),
                invalidVersionNumberFieldRegexString = "([\\d.]+)",
                cloudConfig = null,
                _enableHubUuidListString = "hub3",
                startRaw = null
            ),
            AppEntity(
                name = "App3_Complex",
                appId = mapOf(
                    "android" to "com.complex.app",
                    "github" to "user/repo",
                    "fdroid" to "com.complex.fdroid"
                ),
                invalidVersionNumberFieldRegexString = "v?([\\d.]+(?:\\.[\\d]+)*)",
                cloudConfig = null,
                _enableHubUuidListString = "hub4 hub5 hub6",
                startRaw = true
            )
        )
        
        // Insert test data
        val appDao = database.appDao()
        testApps.forEach { app ->
            appDao.insert(app)
        }
        
        // Retrieve and verify data
        val retrievedApps = appDao.loadAll()
        assertEquals("All apps should be retrieved", testApps.size, retrievedApps.size)
        
        // Verify each app's data integrity
        testApps.forEach { originalApp ->
            val retrievedApp = retrievedApps.find { it.name == originalApp.name }
            assertNotNull("App ${originalApp.name} should be retrieved", retrievedApp)
            
            retrievedApp?.let {
                assertEquals("AppId should match", originalApp.appId, it.appId)
                assertEquals("Version regex should match", originalApp.invalidVersionNumberFieldRegexString, it.invalidVersionNumberFieldRegexString)
                assertEquals("Cloud config should match", originalApp.cloudConfig, it.cloudConfig)
                assertEquals("Hub UUIDs should match", originalApp._enableHubUuidListString, it._enableHubUuidListString)
                assertEquals("Star status should match", originalApp.star, it.star)
            }
        }
    }
    
    @Test
    @Throws(IOException::class)
    fun testHubEntityDataIntegrity() = runBlocking {
        // Create test hub entities
        val testHubs = listOf(
            HubEntity(
                uuid = UUID.randomUUID().toString(),
                hubConfig = HubConfigGson(
                    baseVersion = 6,
                    configVersion = 1,
                    uuid = UUID.randomUUID().toString(),
                    info = HubConfigGson.InfoBean(hubName = "GitHub Hub")
                ),
                auth = mutableMapOf("token" to "github_token_123"),
                ignoreAppIdList = coroutinesMutableListOf(true)
            ),
            HubEntity(
                uuid = UUID.randomUUID().toString(),
                hubConfig = HubConfigGson(
                    baseVersion = 6,
                    configVersion = 1,
                    uuid = UUID.randomUUID().toString(),
                    info = HubConfigGson.InfoBean(hubName = "F-Droid Hub")
                ),
                auth = mutableMapOf(),
                ignoreAppIdList = coroutinesMutableListOf<Map<String, String?>>(true).apply {
                    add(mapOf("id" to "com.ignore.app1"))
                    add(mapOf("id" to "com.ignore.app2"))
                }
            ),
            HubEntity(
                uuid = UUID.randomUUID().toString(),
                hubConfig = HubConfigGson(
                    baseVersion = 6,
                    configVersion = 1,
                    uuid = UUID.randomUUID().toString(),
                    info = HubConfigGson.InfoBean(hubName = "Custom Hub")
                ),
                auth = mutableMapOf(
                    "username" to "user123",
                    "password" to "pass456",
                    "api_key" to "key789"
                ),
                ignoreAppIdList = coroutinesMutableListOf<Map<String, String?>>(true).apply {
                    add(mapOf("id" to "ignore1"))
                    add(mapOf("id" to "ignore2"))
                    add(mapOf("id" to "ignore3"))
                }
            )
        )
        
        // Insert test data
        val hubDao = database.hubDao()
        testHubs.forEach { hub ->
            hubDao.insert(hub)
        }
        
        // Retrieve and verify data
        val retrievedHubs = hubDao.loadAll()
        assertEquals("All hubs should be retrieved", testHubs.size, retrievedHubs.size)
        
        // Verify each hub's data integrity
        testHubs.forEach { originalHub ->
            val retrievedHub = retrievedHubs.find { it.uuid == originalHub.uuid }
            assertNotNull("Hub ${originalHub.hubConfig.info.hubName} should be retrieved", retrievedHub)
            
            retrievedHub?.let {
                assertEquals("Hub name should match", originalHub.hubConfig.info.hubName, it.hubConfig.info.hubName)
                assertEquals("Hub config should match", originalHub.hubConfig.uuid, it.hubConfig.uuid)
                assertEquals("Auth should match", originalHub.auth, it.auth)
                assertEquals("Ignore list should match", originalHub.ignoreAppIdList, it.ignoreAppIdList)
            }
        }
    }
    
    @Test
    fun testAppHubRelationships() = runBlocking {
        // Create hub
        val hubUuid = UUID.randomUUID().toString()
        val hub = HubEntity(
            uuid = hubUuid,
            hubConfig = HubConfigGson(
                baseVersion = 6,
                configVersion = 1,
                uuid = hubUuid,
                info = HubConfigGson.InfoBean(hubName = "Test Hub")
            ),
            auth = mutableMapOf(),
            ignoreAppIdList = coroutinesMutableListOf(true)
        )
        
        // Create apps linked to hub
        val app1 = AppEntity(
            name = "App with Hub",
            appId = mapOf("test" to "com.test.app"),
            invalidVersionNumberFieldRegexString = "v([\\d.]+)",
            _enableHubUuidListString = hubUuid,
            startRaw = null
        )
        
        val app2 = AppEntity(
            name = "App with Multiple Hubs",
            appId = mapOf("test" to "com.test.multi"),
            invalidVersionNumberFieldRegexString = "v([\\d.]+)",
            _enableHubUuidListString = "$hubUuid other-hub-uuid",
            startRaw = true
        )
        
        // Insert data
        database.hubDao().insert(hub)
        database.appDao().insert(app1)
        database.appDao().insert(app2)
        
        // Verify relationships
        val retrievedApps = database.appDao().loadAll()
        val appsWithHub = retrievedApps.filter { it._enableHubUuidListString?.contains(hubUuid) == true }
        
        assertEquals("Two apps should be linked to the hub", 2, appsWithHub.size)
        assertTrue("App1 should be linked to hub", 
            appsWithHub.any { it.name == "App with Hub" })
        assertTrue("App2 should be linked to hub", 
            appsWithHub.any { it.name == "App with Multiple Hubs" })
    }
    
    @Test
    fun testComplexDataTypes() = runBlocking {
        // Test complex data type conversions
        val complexApp = AppEntity(
            name = "ComplexApp",
            appId = mapOf(
                "key1" to "value1",
                "key2" to null,  // Test null values
                "key3" to "",    // Test empty strings
                "key4" to "value with spaces",
                "key5" to "value/with/slashes",
                "key6" to "value:with:colons"
            ),
            invalidVersionNumberFieldRegexString = "(?:v|version)?([\\d.]+(?:-[\\w.]+)?)",
            cloudConfig = null,
            _enableHubUuidListString = (1..10).map { UUID.randomUUID().toString() }.joinToString(" "),
            startRaw = true
        )
        
        // Insert and retrieve
        database.appDao().insert(complexApp)
        val retrieved = database.appDao().loadAll().find { it.name == "ComplexApp" }
        
        assertNotNull("Complex app should be retrieved", retrieved)
        retrieved?.let {
            assertEquals("Complex appId should match", complexApp.appId, it.appId)
            assertEquals("Complex regex should match", complexApp.invalidVersionNumberFieldRegexString, it.invalidVersionNumberFieldRegexString)
            assertEquals("Complex cloud config should match", 
                complexApp.cloudConfig, it.cloudConfig)
            assertEquals("Multiple hub UUIDs should match", 
                complexApp._enableHubUuidListString, it._enableHubUuidListString)
        }
    }
    
    @Test
    fun testDataConsistencyAfterUpdate() = runBlocking {
        // Create initial app
        val initialApp = AppEntity(
            name = "UpdateTest",
            appId = mapOf("test" to "com.test.update"),
            invalidVersionNumberFieldRegexString = "v1.0",
            cloudConfig = null,
            _enableHubUuidListString = "hub1",
            startRaw = null
        )
        
        // Insert initial
        database.appDao().insert(initialApp)
        
        // Update the app
        val updatedApp = initialApp.copy(
            invalidVersionNumberFieldRegexString = "v2.0",
            _enableHubUuidListString = "hub1 hub2 hub3",
            startRaw = true
        )
        database.appDao().update(updatedApp)
        
        // Verify update
        val retrieved = database.appDao().loadAll().find { it.name == "UpdateTest" }
        assertNotNull("Updated app should be retrieved", retrieved)
        retrieved?.let {
            assertEquals("Version regex should be updated", "v2.0", it.invalidVersionNumberFieldRegexString)
            assertEquals("Hub UUIDs should be updated", 
                "hub1 hub2 hub3", it._enableHubUuidListString)
            assertTrue("Star status should be updated", it.star)
        }
    }
}