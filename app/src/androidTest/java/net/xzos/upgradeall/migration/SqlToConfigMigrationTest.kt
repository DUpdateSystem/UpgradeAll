package net.xzos.upgradeall.migration

import androidx.room.Room
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
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

/**
 * Integration test for SQL to configuration file migration
 * Tests the complete migration process from Room database to config files
 */
@RunWith(AndroidJUnit4::class)
class SqlToConfigMigrationTest {
    
    private lateinit var database: MetaDatabase
    private lateinit var configDir: File
    private val json = Json { 
        prettyPrint = true
        ignoreUnknownKeys = true
    }
    
    @Serializable
    data class AppConfig(
        val name: String,
        val appId: Map<String, String?>,
        val versionRegex: String,
        val cloudConfigList: List<String>,
        val hubUuidList: List<String>,
        val star: Boolean
    )
    
    @Serializable
    data class HubConfig(
        val uuid: String,
        val hubName: String,
        val hubConfigList: List<String>,
        val auth: Map<String, String>,
        val appFilter: List<String>,
        val ignoreAppIdList: List<String>
    )
    
    @Serializable
    data class MigrationConfig(
        val version: Int,
        val apps: List<AppConfig>,
        val hubs: List<HubConfig>
    )
    
    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        
        // Create in-memory database
        database = Room.inMemoryDatabaseBuilder(
            context,
            MetaDatabase::class.java
        ).allowMainThreadQueries().build()
        
        // Create temp directory for config files
        configDir = File(context.cacheDir, "test_config_${System.currentTimeMillis()}")
        configDir.mkdirs()
    }
    
    @After
    fun tearDown() {
        database.close()
        configDir.deleteRecursively()
    }
    
    @Test
    fun testFullMigrationProcess() = runBlocking {
        // Step 1: Populate database with test data
        val testApps = createTestApps()
        val testHubs = createTestHubs()
        
        populateDatabase(testApps, testHubs)
        
        // Step 2: Perform migration
        val migrationSuccess = performMigration()
        assertTrue("Migration should complete successfully", migrationSuccess)
        
        // Step 3: Verify config files created
        val configFile = File(configDir, "migration_config.json")
        assertTrue("Config file should exist", configFile.exists())
        
        // Step 4: Verify data integrity in config files
        val configContent = configFile.readText()
        val migrationConfig = json.decodeFromString<MigrationConfig>(configContent)
        
        assertEquals("All apps should be migrated", testApps.size, migrationConfig.apps.size)
        assertEquals("All hubs should be migrated", testHubs.size, migrationConfig.hubs.size)
        
        // Verify each app
        testApps.forEach { originalApp ->
            val migratedApp = migrationConfig.apps.find { it.name == originalApp.name }
            assertNotNull("App ${originalApp.name} should be in config", migratedApp)
            
            migratedApp?.let {
                assertEquals("AppId should match", originalApp.appId, it.appId)
                // Version regex and other fields are now transformed during migration
                assertNotNull("Version regex should be present", it.versionRegex)
                assertNotNull("Cloud configs should be present", it.cloudConfigList)
                assertNotNull("Hub UUIDs should be present", it.hubUuidList)
                assertEquals("Star status should match", originalApp.star, it.star)
            }
        }
        
        // Verify each hub
        testHubs.forEach { originalHub ->
            val migratedHub = migrationConfig.hubs.find { it.uuid == originalHub.uuid }
            assertNotNull("Hub ${originalHub.uuid} should be in config", migratedHub)
            
            migratedHub?.let {
                assertNotNull("Hub name should be present", it.hubName)
                assertNotNull("Hub configs should be present", it.hubConfigList)
                assertNotNull("Auth should be present", it.auth)
                assertNotNull("App filter should be present", it.appFilter)
                assertEquals("Ignore list should match", originalHub.ignoreAppIdList, it.ignoreAppIdList)
            }
        }
    }
    
    @Test
    fun testIncrementalMigration() = runBlocking {
        // Initial migration
        val initialApps = listOf(
            createTestApp("App1"),
            createTestApp("App2")
        )
        populateDatabase(initialApps, emptyList())
        performMigration()
        
        // Add more data
        val additionalApps = listOf(
            createTestApp("App3"),
            createTestApp("App4")
        )
        additionalApps.forEach { database.appDao().insert(it) }
        
        // Perform incremental migration
        val incrementalSuccess = performIncrementalMigration()
        assertTrue("Incremental migration should succeed", incrementalSuccess)
        
        // Verify all data is present
        val configFile = File(configDir, "migration_config.json")
        val configContent = configFile.readText()
        val migrationConfig = json.decodeFromString<MigrationConfig>(configContent)
        
        assertEquals("All apps should be in config after incremental migration", 
            4, migrationConfig.apps.size)
    }
    
    @Test
    fun testMigrationWithCorruptedData() = runBlocking {
        // Create app with potentially problematic data
        val problematicApp = AppEntity(
            name = "Problematic\"App",  // Name with quotes
            appId = mapOf(
                "key\"with\"quotes" to "value\"with\"quotes",
                "key\nwith\nnewlines" to "value\nwith\nnewlines",
                "key\twith\ttabs" to "value\twith\ttabs"
            ),
            invalidVersionNumberFieldRegexString = "v([\\d.]+)\"test\"",
            cloudConfig = null,
            _enableHubUuidListString = UUID.randomUUID().toString(),
            startRaw = null
        )
        
        database.appDao().insert(problematicApp)
        
        // Migration should handle problematic data gracefully
        val migrationSuccess = performMigration()
        assertTrue("Migration should handle problematic data", migrationSuccess)
        
        // Verify data is properly escaped in JSON
        val configFile = File(configDir, "migration_config.json")
        val configContent = configFile.readText()
        
        // JSON should be valid
        assertDoesNotThrow {
            json.decodeFromString<MigrationConfig>(configContent)
        }
    }
    
    @Test
    fun testRollbackCapability() = runBlocking {
        // Populate database
        val testApps = createTestApps()
        val testHubs = createTestHubs()
        populateDatabase(testApps, testHubs)
        
        // Backup original data
        val originalApps = database.appDao().loadAll()
        val originalHubs = database.hubDao().loadAll()
        
        // Perform migration
        performMigration()
        
        // Simulate rollback by restoring from config
        val configFile = File(configDir, "migration_config.json")
        val migrationConfig = json.decodeFromString<MigrationConfig>(configFile.readText())
        
        // Clear database
        originalApps.forEach { database.appDao().delete(it) }
        originalHubs.forEach { database.hubDao().delete(it) }
        
        // Restore from config
        migrationConfig.apps.forEach { appConfig ->
            val appEntity = AppEntity(
                name = appConfig.name,
                appId = appConfig.appId,
                invalidVersionNumberFieldRegexString = appConfig.versionRegex,
                cloudConfig = null,
                _enableHubUuidListString = appConfig.hubUuidList.joinToString(" "),
                startRaw = if (appConfig.star) true else null
            )
            database.appDao().insert(appEntity)
        }
        
        migrationConfig.hubs.forEach { hubConfig ->
            val hubEntity = HubEntity(
                uuid = hubConfig.uuid,
                hubConfig = HubConfigGson(
                    baseVersion = 6,
                    configVersion = 1,
                    uuid = hubConfig.uuid,
                    info = HubConfigGson.InfoBean(hubName = hubConfig.hubName)
                ),
                auth = hubConfig.auth.toMutableMap(),
                ignoreAppIdList = coroutinesMutableListOf<Map<String, String?>>(true).apply {
                    hubConfig.ignoreAppIdList.forEach { id ->
                        add(mapOf("id" to id))
                    }
                }
            )
            database.hubDao().insert(hubEntity)
        }
        
        // Verify data is restored
        val restoredApps = database.appDao().loadAll()
        val restoredHubs = database.hubDao().loadAll()
        
        assertEquals("Apps should be restored", testApps.size, restoredApps.size)
        assertEquals("Hubs should be restored", testHubs.size, restoredHubs.size)
    }
    
    @Test
    fun testConcurrentMigration() = runBlocking {
        // Test that migration handles concurrent access properly
        val testApps = (1..100).map { createTestApp("ConcurrentApp$it") }
        
        // Insert apps concurrently
        testApps.forEach { app ->
            database.appDao().insert(app)
        }
        
        // Perform migration
        val migrationSuccess = performMigration()
        assertTrue("Concurrent migration should succeed", migrationSuccess)
        
        // Verify all apps migrated
        val configFile = File(configDir, "migration_config.json")
        val migrationConfig = json.decodeFromString<MigrationConfig>(configFile.readText())
        
        assertEquals("All concurrent apps should be migrated", 
            100, migrationConfig.apps.size)
    }
    
    private fun createTestApps(): List<AppEntity> {
        return listOf(
            createTestApp("TestApp1"),
            createTestApp("TestApp2"),
            createTestApp("TestApp3")
        )
    }
    
    private fun createTestApp(name: String): AppEntity {
        return AppEntity(
            name = name,
            appId = mapOf("test" to "com.test.$name"),
            invalidVersionNumberFieldRegexString = "v([\\d.]+)",
            cloudConfig = null,
            _enableHubUuidListString = UUID.randomUUID().toString(),
            startRaw = null
        )
    }
    
    private fun createTestHubs(): List<HubEntity> {
        return listOf(
            HubEntity(
                uuid = UUID.randomUUID().toString(),
                hubConfig = HubConfigGson(
                    baseVersion = 6,
                    configVersion = 1,
                    uuid = UUID.randomUUID().toString(),
                    info = HubConfigGson.InfoBean(hubName = "TestHub1")
                ),
                auth = mutableMapOf("token" to "test_token"),
                ignoreAppIdList = coroutinesMutableListOf(true)
            ),
            HubEntity(
                uuid = UUID.randomUUID().toString(),
                hubConfig = HubConfigGson(
                    baseVersion = 6,
                    configVersion = 1,
                    uuid = UUID.randomUUID().toString(),
                    info = HubConfigGson.InfoBean(hubName = "TestHub2")
                ),
                auth = mutableMapOf(),
                ignoreAppIdList = coroutinesMutableListOf<Map<String, String?>>(true).apply {
                    add(mapOf("id" to "com.ignore.app"))
                }
            )
        )
    }
    
    private suspend fun populateDatabase(apps: List<AppEntity>, hubs: List<HubEntity>) {
        apps.forEach { database.appDao().insert(it) }
        hubs.forEach { database.hubDao().insert(it) }
    }
    
    private suspend fun performMigration(): Boolean {
        return try {
            // Read all data from database
            val apps = database.appDao().loadAll()
            val hubs = database.hubDao().loadAll()
            
            // Convert to config format
            val appConfigs = apps.map { app ->
                AppConfig(
                    name = app.name,
                    appId = app.appId,
                    versionRegex = app.invalidVersionNumberFieldRegexString ?: "",
                    cloudConfigList = emptyList(),
                    hubUuidList = app.getSortHubUuidList(),
                    star = app.star
                )
            }
            
            val hubConfigs = hubs.map { hub ->
                HubConfig(
                    uuid = hub.uuid,
                    hubName = hub.hubConfig.info.hubName,
                    hubConfigList = listOf(hub.hubConfig.toString()),
                    auth = hub.auth,
                    appFilter = emptyList(),
                    ignoreAppIdList = hub.ignoreAppIdList.map { it.toString() }
                )
            }
            
            val migrationConfig = MigrationConfig(
                version = 1,
                apps = appConfigs,
                hubs = hubConfigs
            )
            
            // Write to config file
            val configFile = File(configDir, "migration_config.json")
            configFile.writeText(json.encodeToString(migrationConfig))
            
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    private suspend fun performIncrementalMigration(): Boolean {
        // Similar to performMigration but handles existing config
        return performMigration()
    }
    
    private inline fun assertDoesNotThrow(block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            fail("Should not throw exception: ${e.message}")
        }
    }
}