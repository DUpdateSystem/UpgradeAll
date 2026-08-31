package net.xzos.upgradeall

import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
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
    private val packageInstallerExecutor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    @Volatile
    private var runtimeEventSink: EventChannel.EventSink? = null
    @Volatile
    private var androidPackageInstallerEventSink: EventChannel.EventSink? = null
    @Volatile
    private var activePackageInstallerSessionId: Int? = null
    private val androidPackageInstallerListener: (Intent) -> Unit = { intent ->
        mainHandler.post { handleAndroidPackageInstallerCallback(intent) }
    }
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

        EventChannel(
            flutterEngine.dartExecutor.binaryMessenger,
            ANDROID_PACKAGE_INSTALLER_EVENT_CHANNEL,
        ).setStreamHandler(
            object : EventChannel.StreamHandler {
                override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
                    androidPackageInstallerEventSink = events
                }

                override fun onCancel(arguments: Any?) {
                    androidPackageInstallerEventSink = null
                }
            },
        )
        AndroidPackageInstallerEvents.add(androidPackageInstallerListener)

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

                "prepareInstallTask" -> runGetterBridge(result) {
                    val args = call.arguments as? Map<*, *> ?: emptyMap<Any?, Any?>()
                    nativeLib.prepareInstallTask(
                        GetterBridgeRequestBuilder.prepareInstallTaskRequest(
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
            ANDROID_PACKAGE_INSTALLER_CHANNEL,
        ).setMethodCallHandler { call, result ->
            when (call.method) {
                "authorizationStatus" -> result.success(
                    mapOf("authorized" to canRequestPackageInstalls()),
                )

                "requestAuthorization" -> requestPackageInstallAuthorization(result)

                "install" -> runAndroidPackageInstaller(call, result)

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
        AndroidPackageInstallerEvents.remove(androidPackageInstallerListener)
        androidPackageInstallerEventSink = null
        legacyMigrationExecutor.shutdown()
        getterBridgeExecutor.shutdown()
        packageInstallerExecutor.shutdown()
        super.onDestroy()
    }

    private fun canRequestPackageInstalls(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
            packageManager.canRequestPackageInstalls()
    }

    private fun requestPackageInstallAuthorization(result: MethodChannel.Result) {
        if (canRequestPackageInstalls()) {
            result.success(mapOf("authorized" to true, "settings_shown" to false))
            return
        }
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:$packageName"),
        )
        if (intent.resolveActivity(packageManager) == null) {
            result.error(
                "package_installer.authorization_unavailable",
                "Unknown-app-source settings are unavailable",
                null,
            )
            return
        }
        startActivity(intent)
        result.success(mapOf("authorized" to false, "settings_shown" to true))
    }

    private fun runAndroidPackageInstaller(call: MethodCall, result: MethodChannel.Result) {
        if (!canRequestPackageInstalls()) {
            result.error(
                "package_installer.authorization_required",
                "Unknown-app-source authorization is required",
                null,
            )
            return
        }
        val request = try {
            AndroidPackageInstallerContract.parseRequest(
                call.arguments as? Map<*, *> ?: emptyMap<Any?, Any?>(),
            )
        } catch (error: IllegalArgumentException) {
            result.error(
                "package_installer.invalid_request",
                error.message ?: "Invalid PackageInstaller request",
                null,
            )
            return
        }
        val apk = File(request.apkPath)
        if (!apk.isFile) {
            result.error(
                "package_installer.apk_unavailable",
                "Getter's staged APK is unavailable",
                null,
            )
            return
        }
        synchronized(this) {
            if (activePackageInstallerSessionId != null) {
                result.error(
                    "package_installer.busy",
                    "Another package installation is already active",
                    null,
                )
                return
            }
            activePackageInstallerSessionId = PREPARING_PACKAGE_INSTALL_SESSION
        }
        packageInstallerExecutor.execute {
            val installer = packageManager.packageInstaller
            var sessionId: Int? = null
            try {
                val sessionParams = PackageInstaller.SessionParams(
                    PackageInstaller.SessionParams.MODE_FULL_INSTALL,
                ).apply {
                    setAppPackageName(request.packageName)
                    setSize(apk.length())
                }
                sessionId = installer.createSession(sessionParams)
                activePackageInstallerSessionId = sessionId
                installer.openSession(sessionId).use { session ->
                    apk.inputStream().use { input ->
                        session.openWrite("base.apk", 0, apk.length()).use { output ->
                            input.copyTo(output)
                            session.fsync(output)
                        }
                    }
                    val callbackIntent = Intent(
                        applicationContext,
                        AndroidPackageInstallerReceiver::class.java,
                    ).apply {
                        action = ANDROID_PACKAGE_INSTALLER_CALLBACK_ACTION
                        setPackage(packageName)
                        putExtra(EXTRA_EXPECTED_SESSION_ID, sessionId)
                    }
                    val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            PendingIntent.FLAG_MUTABLE
                        } else {
                            0
                        }
                    val callback = PendingIntent.getBroadcast(
                        applicationContext,
                        sessionId,
                        callbackIntent,
                        flags,
                    )
                    session.commit(callback.intentSender)
                }
                mainHandler.post {
                    result.success(
                        mapOf(
                            "session_id" to sessionId,
                            "package_name" to request.packageName,
                        ),
                    )
                }
            } catch (error: Exception) {
                sessionId?.let {
                    runCatching { installer.abandonSession(it) }
                }
                activePackageInstallerSessionId = null
                mainHandler.post {
                    result.error(
                        "package_installer.commit_failed",
                        error.message ?: "PackageInstaller session failed",
                        null,
                    )
                }
            }
        }
    }

    private fun handleAndroidPackageInstallerCallback(intent: Intent) {
        if (intent.action != ANDROID_PACKAGE_INSTALLER_CALLBACK_ACTION) return
        val status = intent.getIntExtra(
            PackageInstaller.EXTRA_STATUS,
            PackageInstaller.STATUS_FAILURE,
        )
        val sessionId = intent.getIntExtra(
            PackageInstaller.EXTRA_SESSION_ID,
            intent.getIntExtra(EXTRA_EXPECTED_SESSION_ID, -1),
        )
        if (sessionId != activePackageInstallerSessionId) return
        var event: Map<String, Any?>? = null
        if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            val confirmation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_INTENT)
            }
            if (confirmation == null) {
                activePackageInstallerSessionId = null
                event = mapOf(
                    "session_id" to sessionId,
                    "status" to "failed",
                    "status_code" to status,
                    "message" to "PackageInstaller did not provide confirmation UI",
                )
            } else {
                confirmation.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(confirmation)
            }
        } else {
            activePackageInstallerSessionId = null
            event = mapOf(
                "session_id" to sessionId,
                "status" to AndroidPackageInstallerContract.statusName(status),
                "status_code" to status,
                "message" to intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE),
            )
        }
        event?.let { androidPackageInstallerEventSink?.success(it) }
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
        const val ANDROID_PACKAGE_INSTALLER_CHANNEL =
            "net.xzos.upgradeall/package_installer"
        const val ANDROID_PACKAGE_INSTALLER_EVENT_CHANNEL =
            "net.xzos.upgradeall/package_installer_events"
        const val ANDROID_PACKAGE_INSTALLER_CALLBACK_ACTION =
            "net.xzos.upgradeall.PACKAGE_INSTALLER_CALLBACK"
        const val EXTRA_EXPECTED_SESSION_ID = "expected_session_id"
        const val PREPARING_PACKAGE_INSTALL_SESSION = -2
        const val RUNTIME_NOTIFICATION_CHANNEL = "net.xzos.upgradeall/runtime_notifications"
        const val LEGACY_MIGRATION_CHANNEL = "net.xzos.upgradeall/legacy_migration"
        const val LEGACY_ROOM_DB_NAME = "app_metadata_database.db"
    }
}
