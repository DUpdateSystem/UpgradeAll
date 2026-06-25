package net.xzos.upgradeall

import android.os.Handler
import android.os.Looper
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import java.io.File
import java.util.concurrent.Executors
import net.xzos.upgradeall.getter.NativeLib
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : FlutterActivity() {
    private val legacyMigrationExecutor = Executors.newSingleThreadExecutor()
    private val getterBridgeExecutor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val nativeLib by lazy { NativeLib() }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(
            flutterEngine.dartExecutor.binaryMessenger,
            GETTER_BRIDGE_CHANNEL,
        ).setMethodCallHandler { call, result ->
            when (call.method) {
                "initializeBridge" -> runGetterBridge(result) {
                    nativeLib.initializeBridge(applicationContext)
                }

                "previewInstalledAutogen" -> runGetterBridge(result) {
                    nativeLib.previewInstalledAutogen(
                        applicationContext,
                        previewInstalledAutogenRequest(call),
                    )
                }

                "applyInstalledAutogen" -> runGetterBridge(result) {
                    nativeLib.applyInstalledAutogen(applyInstalledAutogenRequest(call))
                }

                "importLegacyRoomDatabase" -> runGetterBridge(result) {
                    nativeLib.importLegacyRoomDatabase(importLegacyRoomDatabaseRequest(call))
                }

                "legacyReportList" -> runGetterBridge(result) {
                    nativeLib.legacyReportList(legacyReportListRequest())
                }

                "runtimeOperation" -> runGetterBridge(result) {
                    nativeLib.runtimeOperation(runtimeOperationRequest(call))
                }

                else -> result.notImplemented()
            }
        }

        MethodChannel(
            flutterEngine.dartExecutor.binaryMessenger,
            LEGACY_MIGRATION_CHANNEL,
        ).setMethodCallHandler { call, result ->
            when (call.method) {
                "prepareLegacyRoomImport" -> {
                    legacyMigrationExecutor.execute {
                        try {
                            val candidate = prepareLegacyRoomImport()
                            mainHandler.post { result.success(candidate) }
                        } catch (error: Exception) {
                            mainHandler.post {
                                result.error(
                                    "legacy.prepare_failed",
                                    error.message ?: "Failed to prepare legacy Room database",
                                    null,
                                )
                            }
                        }
                    }
                }

                else -> result.notImplemented()
            }
        }
    }

    override fun onDestroy() {
        legacyMigrationExecutor.shutdown()
        getterBridgeExecutor.shutdown()
        super.onDestroy()
    }

    private fun runGetterBridge(result: MethodChannel.Result, operation: () -> String) {
        getterBridgeExecutor.execute {
            try {
                val response = operation()
                mainHandler.post { result.success(response) }
            } catch (error: UnsatisfiedLinkError) {
                mainHandler.post {
                    result.error(
                        "bridge.native_unavailable",
                        error.message ?: "Getter native bridge is unavailable",
                        null,
                    )
                }
            } catch (error: Exception) {
                mainHandler.post {
                    result.error(
                        "bridge.call_failed",
                        error.message ?: "Getter native bridge call failed",
                        null,
                    )
                }
            }
        }
    }

    private fun previewInstalledAutogenRequest(call: MethodCall): String {
        val args = call.arguments as? Map<*, *> ?: emptyMap<Any?, Any?>()
        val scanOptions = args["scan_options"] as? Map<*, *> ?: args
        return JSONObject()
            .put("data_dir", getterDataDir().absolutePath)
            .put(
                "scan_options",
                JSONObject()
                    .put(
                        "include_system_apps",
                        scanOptions["include_system_apps"] as? Boolean ?: false,
                    )
                    .put(
                        "include_self",
                        scanOptions["include_self"] as? Boolean ?: false,
                    ),
            )
            .toString()
    }

    private fun applyInstalledAutogenRequest(call: MethodCall): String {
        val args = call.arguments as? Map<*, *> ?: emptyMap<Any?, Any?>()
        val previewJson = args["preview_json"] as? String
            ?: throw IllegalArgumentException("preview_json is required")
        val acceptance = args["acceptance"] as? Map<*, *>
        val packageIds = acceptance
            ?.get("package_ids")
            ?.let { value -> value as? Collection<*> }
            ?.map { value -> value.toString() }
            ?: emptyList<String>()
        return JSONObject()
            .put("data_dir", getterDataDir().absolutePath)
            .put("preview", JSONObject(previewJson))
            .put(
                "acceptance",
                JSONObject()
                    .put("mode", acceptance?.get("mode") as? String ?: "all")
                    .put("package_ids", JSONArray(packageIds)),
            )
            .toString()
    }

    private fun importLegacyRoomDatabaseRequest(call: MethodCall): String {
        val args = call.arguments as? Map<*, *> ?: emptyMap<Any?, Any?>()
        val databasePath = args["database_path"] as? String
            ?: throw IllegalArgumentException("database_path is required")
        return JSONObject()
            .put("data_dir", getterDataDir().absolutePath)
            .put("database_path", databasePath)
            .toString()
    }

    private fun legacyReportListRequest(): String {
        return JSONObject()
            .put("data_dir", getterDataDir().absolutePath)
            .toString()
    }

    private fun runtimeOperationRequest(call: MethodCall): String {
        val args = call.arguments as? Map<*, *>
            ?: throw IllegalArgumentException("runtime operation arguments are required")
        return GetterBridgeRequestBuilder.runtimeOperationRequest(args)
    }

    private fun getterDataDir(): File = File(filesDir, "getter")

    private fun prepareLegacyRoomImport(): Map<String, Any?> {
        val destination = File(
            File(filesDir, "getter-imports/legacy-room"),
            LEGACY_ROOM_DB_NAME,
        )
        return LegacyRoomImportPreparer()
            .prepare(getDatabasePath(LEGACY_ROOM_DB_NAME), destination)
            .toMethodChannelResult()
    }

    private companion object {
        const val GETTER_BRIDGE_CHANNEL = "net.xzos.upgradeall/getter_bridge"
        const val LEGACY_MIGRATION_CHANNEL = "net.xzos.upgradeall/legacy_migration"
        const val LEGACY_ROOM_DB_NAME = "app_metadata_database.db"
    }
}
