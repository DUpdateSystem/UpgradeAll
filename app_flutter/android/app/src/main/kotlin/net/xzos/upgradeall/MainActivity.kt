package net.xzos.upgradeall

import android.os.Handler
import android.os.Looper
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import java.io.File
import java.util.concurrent.Executors
import net.xzos.upgradeall.getter.NativeLib
import org.json.JSONObject

class MainActivity : FlutterActivity() {
    private val legacyMigrationExecutor = Executors.newSingleThreadExecutor()
    private val getterBridgeExecutor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    @Volatile
    private var runtimeEventSink: EventChannel.EventSink? = null
    private val nativeLib by lazy { NativeLib() }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        EventChannel(
            flutterEngine.dartExecutor.binaryMessenger,
            RUNTIME_NOTIFICATION_CHANNEL,
        ).setStreamHandler(
            object : EventChannel.StreamHandler {
                override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
                    runtimeEventSink = events
                    emitRuntimeNotifications()
                }

                override fun onCancel(arguments: Any?) {
                    runtimeEventSink = null
                }
            },
        )

        MethodChannel(
            flutterEngine.dartExecutor.binaryMessenger,
            GETTER_BRIDGE_CHANNEL,
        ).setMethodCallHandler { call, result ->
            when (call.method) {
                "initializeBridge" -> runGetterBridge(result) {
                    nativeLib.initializeBridge(applicationContext)
                }

                "prepareInstall" -> runGetterBridge(result) {
                    val args = call.arguments as? Map<*, *> ?: emptyMap<Any?, Any?>()
                    nativeLib.prepareInstall(
                        GetterBridgeRequestBuilder.prepareInstallRequest(
                            getterDataDir().absolutePath,
                            args,
                        ),
                    )
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

                "previewFreshInstallSetup" -> runGetterBridge(result) {
                    nativeLib.previewFreshInstallSetup(
                        applicationContext,
                        withGetterDataDir(
                            GetterBridgeRequestBuilder.freshInstallSetupPreviewRequest(
                                call.arguments as? Map<*, *> ?: emptyMap<Any?, Any?>(),
                            ),
                        ),
                    )
                }

                "applyFreshInstallSetup" -> runGetterBridge(result) {
                    nativeLib.applyFreshInstallSetup(
                        withGetterDataDir(
                            GetterBridgeRequestBuilder.freshInstallSetupApplyRequest(
                                call.arguments as? Map<*, *> ?: emptyMap<Any?, Any?>(),
                            ),
                        ),
                    )
                }

                "previewInstalledFdroidAutogen" -> runGetterBridge(result) {
                    nativeLib.previewInstalledFdroidAutogen(
                        applicationContext,
                        previewInstalledAutogenRequest(call),
                    )
                }

                "applyInstalledFdroidAutogen" -> runGetterBridge(result) {
                    nativeLib.applyInstalledFdroidAutogen(applyInstalledAutogenRequest(call))
                }

                "previewGithubAutogen" -> runGetterBridge(result) {
                    nativeLib.previewGithubAutogen(
                        applicationContext,
                        previewGithubAutogenRequest(call),
                    )
                }

                "applyGithubAutogen" -> runGetterBridge(result) {
                    nativeLib.applyGithubAutogen(applyGithubAutogenRequest(call))
                }

                "previewFdroidAutogen" -> runGetterBridge(result) {
                    nativeLib.previewFdroidAutogen(previewFdroidAutogenRequest(call))
                }

                "applyFdroidAutogen" -> runGetterBridge(result) {
                    nativeLib.applyFdroidAutogen(applyFdroidAutogenRequest(call))
                }

                "refreshDefaultFdroidCatalogCache" -> runGetterBridge(result) {
                    nativeLib.refreshDefaultFdroidCatalogCache(
                        GetterBridgeRequestBuilder.fdroidCatalogRefreshRequest(
                            getterDataDir().absolutePath,
                        ),
                    )
                }

                "importLegacyRoomDatabase" -> runGetterBridge(result) {
                    nativeLib.importLegacyRoomDatabase(importLegacyRoomDatabaseRequest(call))
                }

                "legacyReportList" -> runGetterBridge(result) {
                    nativeLib.legacyReportList(legacyReportListRequest())
                }

                "startup" -> runGetterBridge(result) {
                    nativeLib.startup(
                        applicationContext,
                        GetterBridgeRequestBuilder.startupRequest(
                            getterDataDir().absolutePath,
                            call.arguments as? Map<*, *> ?: emptyMap<Any?, Any?>(),
                        ),
                    )
                }

                "readOperation" -> runGetterBridge(result) {
                    nativeLib.readOperation(readOperationRequest(call))
                }

                "runtimeOperation" -> runGetterBridge(result, emitRuntimeNotifications = true) {
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

    private fun runGetterBridge(
        result: MethodChannel.Result,
        emitRuntimeNotifications: Boolean = false,
        operation: () -> String,
    ) {
        getterBridgeExecutor.execute {
            try {
                val response = operation()
                val notifications = if (emitRuntimeNotifications) {
                    drainRuntimeNotificationEvents()
                } else {
                    emptyList<String>()
                }
                mainHandler.post {
                    result.success(response)
                    emitRuntimeNotifications(notifications)
                }
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
        return JSONObject(GetterBridgeRequestBuilder.installedAutogenPreviewRequest(args))
            .put("data_dir", getterDataDir().absolutePath)
            .toString()
    }

    private fun applyInstalledAutogenRequest(call: MethodCall): String {
        val args = call.arguments as? Map<*, *> ?: emptyMap<Any?, Any?>()
        return JSONObject(GetterBridgeRequestBuilder.autogenApplyRequest(args))
            .put("data_dir", getterDataDir().absolutePath)
            .toString()
    }

    private fun previewGithubAutogenRequest(call: MethodCall): String {
        val args = call.arguments as? Map<*, *> ?: emptyMap<Any?, Any?>()
        return JSONObject(GetterBridgeRequestBuilder.githubAutogenPreviewRequest(args))
            .put("data_dir", getterDataDir().absolutePath)
            .toString()
    }

    private fun applyGithubAutogenRequest(call: MethodCall): String {
        val args = call.arguments as? Map<*, *> ?: emptyMap<Any?, Any?>()
        return JSONObject(GetterBridgeRequestBuilder.autogenApplyRequest(args))
            .put("data_dir", getterDataDir().absolutePath)
            .toString()
    }

    private fun previewFdroidAutogenRequest(call: MethodCall): String {
        val args = call.arguments as? Map<*, *> ?: emptyMap<Any?, Any?>()
        return JSONObject(GetterBridgeRequestBuilder.fdroidAutogenPreviewRequest(args))
            .put("data_dir", getterDataDir().absolutePath)
            .toString()
    }

    private fun applyFdroidAutogenRequest(call: MethodCall): String {
        val args = call.arguments as? Map<*, *> ?: emptyMap<Any?, Any?>()
        return JSONObject(GetterBridgeRequestBuilder.autogenApplyRequest(args))
            .put("data_dir", getterDataDir().absolutePath)
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

    private fun readOperationRequest(call: MethodCall): String {
        val args = call.arguments as? Map<*, *>
            ?: throw IllegalArgumentException("read operation arguments are required")
        return JSONObject(GetterBridgeRequestBuilder.readOperationRequest(args))
            .put("data_dir", getterDataDir().absolutePath)
            .toString()
    }

    private fun runtimeOperationRequest(call: MethodCall): String {
        val args = call.arguments as? Map<*, *>
            ?: throw IllegalArgumentException("runtime operation arguments are required")
        return JSONObject(GetterBridgeRequestBuilder.runtimeOperationRequest(args))
            .put("data_dir", getterDataDir().absolutePath)
            .toString()
    }

    private fun emitRuntimeNotifications() {
        getterBridgeExecutor.execute {
            val notifications = drainRuntimeNotificationEvents()
            mainHandler.post { emitRuntimeNotifications(notifications) }
        }
    }

    private fun emitRuntimeNotifications(notifications: List<String>) {
        val sink = runtimeEventSink ?: return
        for (notification in notifications) {
            sink.success(notification)
        }
    }

    private fun drainRuntimeNotificationEvents(): List<String> {
        return try {
            val envelope = JSONObject(nativeLib.drainRuntimeNotifications())
            if (!envelope.optBoolean("ok", false)) {
                return emptyList()
            }
            val notifications = envelope
                .getJSONObject("data")
                .getJSONArray("notifications")
            List(notifications.length()) { index -> notifications.getJSONObject(index).toString() }
        } catch (_: UnsatisfiedLinkError) {
            emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun withGetterDataDir(requestJson: String): String = JSONObject(requestJson)
        .put("data_dir", getterDataDir().absolutePath)
        .toString()

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
        const val RUNTIME_NOTIFICATION_CHANNEL = "net.xzos.upgradeall/runtime_notifications"
        const val LEGACY_MIGRATION_CHANNEL = "net.xzos.upgradeall/legacy_migration"
        const val LEGACY_ROOM_DB_NAME = "app_metadata_database.db"
    }
}
