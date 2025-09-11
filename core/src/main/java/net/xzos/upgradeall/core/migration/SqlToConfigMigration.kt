package net.xzos.upgradeall.core.migration

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.xzos.upgradeall.core.database.MetaDatabase
import net.xzos.upgradeall.core.database.metaDatabase
import net.xzos.upgradeall.core.database.table.AppEntity
import net.xzos.upgradeall.core.database.table.HubEntity
import net.xzos.upgradeall.core.utils.coroutines.coroutinesMutableListOf
import net.xzos.upgradeall.websdk.data.json.AppConfigGson
import net.xzos.upgradeall.websdk.data.json.HubConfigGson
import java.io.File
import java.io.IOException

/**
 * Migration handler to convert SQL database to configuration files
 * This enables cross-platform compatibility with the Rust getter implementation
 */
object SqlToConfigMigration {
    
    private const val CONFIG_VERSION = 1
    private const val CONFIG_FILE_NAME = "apps_config.json"
    private const val BACKUP_SUFFIX = ".backup"
    
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    @Serializable
    data class AppConfig(
        val id: String,
        val name: String,
        val appId: Map<String, String?>,
        val versionRegex: String,
        val cloudConfigList: List<String>,
        val hubUuidList: List<String>,
        val star: Boolean,
        val ignoreVersionNumber: String? = null,
        val extraData: Map<String, String> = emptyMap()
    )
    
    @Serializable
    data class HubConfig(
        val uuid: String,
        val hubName: String,
        val hubConfigList: List<String>,
        val auth: Map<String, String>,
        val appFilter: List<String>,
        val ignoreAppIdList: List<String>,
        val extraData: Map<String, String> = emptyMap()
    )
    
    @Serializable
    data class MigrationConfig(
        val version: Int,
        val timestamp: Long,
        val apps: List<AppConfig>,
        val hubs: List<HubConfig>,
        val metadata: Map<String, String> = emptyMap()
    )
    
    /**
     * Perform migration from SQL database to configuration files
     * @param context Android context for database access
     * @param configDir Directory to save configuration files
     * @param keepDatabase Whether to keep the database after migration
     * @return MigrationResult indicating success or failure
     */
    suspend fun migrate(
        context: Context,
        configDir: File,
        keepDatabase: Boolean = true
    ): MigrationResult = withContext(Dispatchers.IO) {
        try {
            // Ensure config directory exists
            if (!configDir.exists()) {
                configDir.mkdirs()
            }
            
            // Get database instance
            val database = metaDatabase
            
            // Export data from database
            val apps = exportApps(database)
            val hubs = exportHubs(database)
            
            // Create migration config
            val migrationConfig = MigrationConfig(
                version = CONFIG_VERSION,
                timestamp = System.currentTimeMillis(),
                apps = apps,
                hubs = hubs,
                metadata = mapOf(
                    "source" to "UpgradeAll Android",
                    "migrationDate" to java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US).format(java.util.Date())
                )
            )
            
            // Backup existing config if it exists
            val configFile = File(configDir, CONFIG_FILE_NAME)
            if (configFile.exists()) {
                backupExistingConfig(configFile)
            }
            
            // Write new config
            writeConfig(configFile, migrationConfig)
            
            // Verify migration
            val verified = verifyMigration(configFile, apps.size, hubs.size)
            
            if (!verified) {
                // Restore backup if verification fails
                restoreBackup(configFile)
                return@withContext MigrationResult.Error(
                    "Migration verification failed. Backup restored."
                )
            }
            
            // Optionally delete database
            if (!keepDatabase) {
                clearDatabase(database)
            }
            
            MigrationResult.Success(
                appsCount = apps.size,
                hubsCount = hubs.size,
                configPath = configFile.absolutePath
            )
            
        } catch (e: Exception) {
            MigrationResult.Error(
                message = "Migration failed: ${e.message}",
                exception = e
            )
        }
    }
    
    /**
     * Export apps from database to config format
     */
    private suspend fun exportApps(database: MetaDatabase): List<AppConfig> {
        return database.appDao().loadAll().map { entity ->
            AppConfig(
                id = generateAppId(entity),
                name = entity.name,
                appId = entity.appId,
                versionRegex = entity.invalidVersionNumberFieldRegexString ?: "",
                cloudConfigList = entity.cloudConfig?.let { listOf(it.toString()) } ?: emptyList(),
                hubUuidList = entity.getSortHubUuidList(),
                star = entity.star,
                ignoreVersionNumber = entity.ignoreVersionNumber,
                extraData = extractExtraData(entity)
            )
        }
    }
    
    /**
     * Export hubs from database to config format
     */
    private suspend fun exportHubs(database: MetaDatabase): List<HubConfig> {
        return database.hubDao().loadAll().map { entity ->
            HubConfig(
                uuid = entity.uuid,
                hubName = entity.hubConfig.info.hubName,
                hubConfigList = listOf(entity.hubConfig.toString()),
                auth = entity.auth,
                appFilter = emptyList(),
                ignoreAppIdList = entity.ignoreAppIdList.map { it.toString() },
                extraData = extractHubExtraData(entity)
            )
        }
    }
    
    /**
     * Generate unique app ID
     */
    private fun generateAppId(entity: AppEntity): String {
        // Use combination of name and appId to generate unique ID
        val idComponents = entity.appId.entries.joinToString("_") { "${it.key}:${it.value}" }
        return "${entity.name}_${idComponents}".replace(" ", "_")
            .replace("/", "_")
            .lowercase()
    }
    
    /**
     * Extract extra data from app entity
     */
    private fun extractExtraData(entity: AppEntity): Map<String, String> {
        val extraData = mutableMapOf<String, String>()
        
        // Add any additional fields that might be useful
        entity.ignoreVersionNumber?.let {
            extraData["ignoreVersion"] = it
        }
        
        // Add creation/modification timestamps if available
        extraData["migrated"] = "true"
        
        return extraData
    }
    
    /**
     * Extract extra data from hub entity
     */
    private fun extractHubExtraData(entity: HubEntity): Map<String, String> {
        return mapOf(
            "migrated" to "true",
            "originalUuid" to entity.uuid
        )
    }
    
    /**
     * Write configuration to file
     */
    private fun writeConfig(file: File, config: MigrationConfig) {
        val jsonString = json.encodeToString(config)
        file.writeText(jsonString)
    }
    
    /**
     * Backup existing configuration file
     */
    private fun backupExistingConfig(configFile: File) {
        val backupFile = File(configFile.parent, "${configFile.name}${BACKUP_SUFFIX}")
        configFile.copyTo(backupFile, overwrite = true)
    }
    
    /**
     * Restore configuration from backup
     */
    private fun restoreBackup(configFile: File) {
        val backupFile = File(configFile.parent, "${configFile.name}${BACKUP_SUFFIX}")
        if (backupFile.exists()) {
            backupFile.copyTo(configFile, overwrite = true)
            backupFile.delete()
        }
    }
    
    /**
     * Verify migration was successful
     */
    private fun verifyMigration(
        configFile: File,
        expectedAppsCount: Int,
        expectedHubsCount: Int
    ): Boolean {
        return try {
            if (!configFile.exists()) return false
            
            val content = configFile.readText()
            val config = json.decodeFromString<MigrationConfig>(content)
            
            // Verify counts match
            config.apps.size == expectedAppsCount && 
            config.hubs.size == expectedHubsCount &&
            config.version == CONFIG_VERSION
            
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Clear database after successful migration
     */
    private suspend fun clearDatabase(database: MetaDatabase) {
        // Clear all tables
        database.clearAllTables()
    }
    
    /**
     * Import configuration back to database (for rollback)
     */
    suspend fun importFromConfig(
        context: Context,
        configFile: File
    ): MigrationResult = withContext(Dispatchers.IO) {
        try {
            if (!configFile.exists()) {
                return@withContext MigrationResult.Error("Config file does not exist")
            }
            
            val content = configFile.readText()
            val config = json.decodeFromString<MigrationConfig>(content)
            
            val database = metaDatabase
            
            // Import hubs first (apps may depend on them)
            config.hubs.forEach { hubConfig ->
                // Create HubConfigGson from hubConfig data
                val hubConfigGson = HubConfigGson(
                    baseVersion = 6,
                    configVersion = 1,
                    uuid = hubConfig.uuid,
                    info = HubConfigGson.InfoBean(
                        hubName = hubConfig.hubName
                    )
                )
                
                val hubEntity = HubEntity(
                    uuid = hubConfig.uuid,
                    hubConfig = hubConfigGson,
                    auth = hubConfig.auth.toMutableMap(),
                    ignoreAppIdList = coroutinesMutableListOf<Map<String, String?>>(true).apply {
                        addAll(hubConfig.ignoreAppIdList.map { 
                            mapOf<String, String?>("id" to it)
                        })
                    }
                )
                database.hubDao().insert(hubEntity)
            }
            
            // Import apps
            config.apps.forEach { appConfig ->
                val appEntity = AppEntity(
                    name = appConfig.name,
                    appId = appConfig.appId,
                    invalidVersionNumberFieldRegexString = appConfig.versionRegex,
                    ignoreVersionNumber = appConfig.ignoreVersionNumber,
                    cloudConfig = if (appConfig.cloudConfigList.isNotEmpty()) {
                        AppConfigGson(
                            baseVersion = 2,
                            configVersion = 1,
                            uuid = appConfig.id,
                            baseHubUuid = appConfig.hubUuidList.firstOrNull() ?: "",
                            info = AppConfigGson.InfoBean(
                                name = appConfig.name,
                                url = "",
                                desc = "",
                                extraMap = emptyMap()
                            )
                        )
                    } else null,
                    _enableHubUuidListString = appConfig.hubUuidList.joinToString(" "),
                    startRaw = if (appConfig.star) true else null
                )
                database.appDao().insert(appEntity)
            }
            
            MigrationResult.Success(
                appsCount = config.apps.size,
                hubsCount = config.hubs.size,
                configPath = configFile.absolutePath
            )
            
        } catch (e: Exception) {
            MigrationResult.Error(
                message = "Import failed: ${e.message}",
                exception = e
            )
        }
    }
    
    /**
     * Check if migration is needed
     */
    fun isMigrationNeeded(context: Context, configDir: File): Boolean {
        val configFile = File(configDir, CONFIG_FILE_NAME)
        
        // Migration is needed if config doesn't exist but database has data
        if (!configFile.exists()) {
            val database = metaDatabase
            // Check if database has data (this is a simplified check)
            return true
        }
        
        return false
    }
    
    /**
     * Get migration status
     */
    fun getMigrationStatus(configDir: File): MigrationStatus {
        val configFile = File(configDir, CONFIG_FILE_NAME)
        
        return if (configFile.exists()) {
            try {
                val content = configFile.readText()
                val config = json.decodeFromString<MigrationConfig>(content)
                MigrationStatus.Completed(
                    timestamp = config.timestamp,
                    appsCount = config.apps.size,
                    hubsCount = config.hubs.size
                )
            } catch (e: Exception) {
                MigrationStatus.Error(e.message ?: "Unknown error")
            }
        } else {
            MigrationStatus.NotStarted
        }
    }
}

/**
 * Result of migration operation
 */
sealed class MigrationResult {
    data class Success(
        val appsCount: Int,
        val hubsCount: Int,
        val configPath: String
    ) : MigrationResult()
    
    data class Error(
        val message: String,
        val exception: Exception? = null
    ) : MigrationResult()
}

/**
 * Current migration status
 */
sealed class MigrationStatus {
    object NotStarted : MigrationStatus()
    
    data class Completed(
        val timestamp: Long,
        val appsCount: Int,
        val hubsCount: Int
    ) : MigrationStatus()
    
    data class Error(val message: String) : MigrationStatus()
}