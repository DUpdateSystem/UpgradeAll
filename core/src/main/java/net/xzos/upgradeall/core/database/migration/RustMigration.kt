package net.xzos.upgradeall.core.database.migration

import net.xzos.upgradeall.core.database.metaDatabase
import net.xzos.upgradeall.core.database.table.AppEntity
import net.xzos.upgradeall.core.database.table.HubEntity
import net.xzos.upgradeall.core.database.table.extra_hub.ExtraHubEntity
import net.xzos.upgradeall.core.utils.log.Log
import net.xzos.upgradeall.core.utils.log.ObjectTag
import net.xzos.upgradeall.core.utils.log.ObjectTag.Companion.core
import net.xzos.upgradeall.core.websdk.getterPort
import net.xzos.upgradeall.getter.AppManagerProxy
import net.xzos.upgradeall.getter.ExtraHubProxy
import net.xzos.upgradeall.getter.HubManagerProxy
import net.xzos.upgradeall.getter.rpc.AppConfig
import net.xzos.upgradeall.getter.rpc.AppConfigInfo
import net.xzos.upgradeall.getter.rpc.AppRecord
import net.xzos.upgradeall.getter.rpc.ExtraHubRecord
import net.xzos.upgradeall.getter.rpc.HubConfig
import net.xzos.upgradeall.getter.rpc.HubConfigInfo
import net.xzos.upgradeall.getter.rpc.HubRecord
import java.io.File

private const val TAG = "RustMigration"
private val logObjectTag = ObjectTag(core, TAG)

/**
 * Migrates data from the Room/SQLite database to the Rust JSONL store.
 *
 * This should be called on the first launch after upgrading to the Rust-backed storage.
 * It is a no-op if the JSONL store already has data, or if the Room DB is empty.
 */
suspend fun migrateRoomToRust(rustDataDir: File) {
    val appsFile = File(rustDataDir, "apps.jsonl")
    if (appsFile.exists() && appsFile.length() > 0) {
        Log.d(logObjectTag, TAG, "JSONL store already populated, skipping migration")
        return
    }

    val apps = metaDatabase.appDao().loadAll()
    val hubs = metaDatabase.hubDao().loadAll()
    // ExtraHubDao has no loadAll; query each known hub UUID plus the GLOBAL sentinel
    val extraHubIds = hubs.map { it.uuid } + listOf("GLOBAL")
    val extraHubs =
        extraHubIds.mapNotNull { id ->
            metaDatabase.extraHubDao().loadByUuid(id)
        }

    if (apps.isEmpty() && hubs.isEmpty()) {
        Log.d(logObjectTag, TAG, "Room DB is empty, nothing to migrate")
        return
    }

    Log.i(logObjectTag, TAG, "Starting Room -> Rust migration: ${apps.size} apps, ${hubs.size} hubs")

    val service = getterPort.getService()
    val appProxy = AppManagerProxy(service)
    val hubProxy = HubManagerProxy(service)
    val extraHubProxy = ExtraHubProxy(service)

    hubs.forEach { entity ->
        val record = entity.toHubRecord()
        val ok = hubProxy.saveHub(record)
        if (!ok) Log.w(logObjectTag, TAG, "Failed to migrate hub: ${entity.uuid}")
    }

    extraHubs.forEach { entity ->
        val record = entity.toExtraHubRecord()
        val ok = extraHubProxy.saveExtraHub(record)
        if (!ok) Log.w(logObjectTag, TAG, "Failed to migrate extra_hub: ${entity.id}")
    }

    apps.forEach { entity ->
        val record = entity.toAppRecord()
        val saved = appProxy.saveApp(record)
        if (saved.id.isEmpty()) Log.w(logObjectTag, TAG, "Failed to migrate app: ${entity.name}")
    }

    Log.i(logObjectTag, TAG, "Migration complete")
}

// ---------------------------------------------------------------------------
// Conversion helpers
// ---------------------------------------------------------------------------

private fun AppEntity.toAppRecord(): AppRecord {
    val cfg = cloudConfig
    return AppRecord(
        id = "", // let Rust assign a new UUID
        name = name,
        appId = appId,
        invalidVersionNumberFieldRegex = invalidVersionNumberFieldRegexString,
        includeVersionNumberFieldRegex = includeVersionNumberFieldRegexString,
        ignoreVersionNumber = ignoreVersionNumber,
        cloudConfig =
            cfg?.let { c ->
                AppConfig(
                    baseVersion = c.baseVersion,
                    configVersion = c.configVersion,
                    uuid = c.uuid,
                    baseHubUuid = c.baseHubUuid,
                    info =
                        AppConfigInfo(
                            name = c.info.name,
                            url = c.info.url,
                            desc = c.info.desc,
                            extraMap = c.info.extraMap,
                        ),
                )
            },
        enableHubList = _enableHubUuidListString,
        star = if (startRaw == true) true else null,
    )
}

private fun HubEntity.toHubRecord(): HubRecord {
    val cfg = hubConfig
    return HubRecord(
        uuid = uuid,
        hubConfig =
            HubConfig(
                baseVersion = cfg.baseVersion,
                configVersion = cfg.configVersion,
                uuid = cfg.uuid,
                info =
                    HubConfigInfo(
                        hubName = cfg.info.hubName,
                        hubIconUrl = cfg.info.hubIconUrl,
                    ),
                apiKeywords = cfg.apiKeywords,
                appUrlTemplates = cfg.appUrlTemplates,
                targetCheckApi = cfg.targetCheckApi,
            ),
        auth = auth,
        ignoreAppIdList = ignoreAppIdList.toList(),
        applicationsMode = _applicationsMode,
        userIgnoreAppIdList = userIgnoreAppIdList.toList(),
        sortPoint = __sortPoint,
    )
}

private fun ExtraHubEntity.toExtraHubRecord(): ExtraHubRecord =
    ExtraHubRecord(
        id = id,
        enableGlobal = global,
        urlReplaceSearch = urlReplaceSearch,
        urlReplaceString = urlReplaceString,
    )
