package net.xzos.upgradeall.getter

import kotlinx.coroutines.runBlocking
import net.xzos.upgradeall.getter.rpc.AppRecord
import net.xzos.upgradeall.getter.rpc.AppStatus
import net.xzos.upgradeall.getter.rpc.ExtraHubRecord
import net.xzos.upgradeall.getter.rpc.GetterService
import net.xzos.upgradeall.getter.rpc.HubRecord

/**
 * Kotlin-side proxy for the Rust AppManager.
 *
 * Delegates all operations to the Rust getter via JSON-RPC.
 * This replaces the Kotlin AppManager that used Room/SQLite.
 */
class AppManagerProxy(
    private val service: GetterService,
) {
    fun getApps(): List<AppRecord> = runBlocking { service.managerGetApps() }

    fun saveApp(record: AppRecord): AppRecord = runBlocking { service.managerSaveApp(record) }

    fun deleteApp(recordId: String): Boolean = runBlocking { service.managerDeleteApp(recordId) }

    fun getAppStatus(recordId: String): AppStatus = runBlocking { service.managerGetAppStatus(recordId) }

    fun setVirtualApps(apps: List<AppRecord>): Boolean = runBlocking { service.managerSetVirtualApps(apps) }

    fun renewAll(): Boolean = runBlocking { service.managerRenewAll() }
}

/**
 * Kotlin-side proxy for the Rust HubManager.
 *
 * Delegates all operations to the Rust getter via JSON-RPC.
 * This replaces the Kotlin HubManager that used Room/SQLite.
 */
class HubManagerProxy(
    private val service: GetterService,
) {
    fun getHubs(): List<HubRecord> = runBlocking { service.managerGetHubs() }

    fun saveHub(record: HubRecord): Boolean = runBlocking { service.managerSaveHub(record) }

    fun deleteHub(hubUuid: String): Boolean = runBlocking { service.managerDeleteHub(hubUuid) }

    fun ignoreApp(
        hubUuid: String,
        appId: Map<String, String?>,
    ): Boolean = runBlocking { service.managerHubIgnoreApp(hubUuid, appId, ignore = true) }

    fun unignoreApp(
        hubUuid: String,
        appId: Map<String, String?>,
    ): Boolean = runBlocking { service.managerHubIgnoreApp(hubUuid, appId, ignore = false) }

    fun setApplicationsMode(
        hubUuid: String,
        enable: Boolean,
    ): Boolean = runBlocking { service.managerSetApplicationsMode(hubUuid, enable) }
}

/**
 * Kotlin-side proxy for ExtraHub configuration.
 */
class ExtraHubProxy(
    private val service: GetterService,
) {
    fun getExtraHubs(): List<ExtraHubRecord> = runBlocking { service.managerGetExtraHubs() }

    fun saveExtraHub(record: ExtraHubRecord): Boolean = runBlocking { service.managerSaveExtraHub(record) }

    fun deleteExtraHub(id: String): Boolean = runBlocking { service.managerDeleteExtraHub(id) }
}
