package net.xzos.upgradeall

import android.database.sqlite.SQLiteDatabase
import android.os.Handler
import android.os.Looper
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import java.io.File
import java.util.concurrent.Executors

class MainActivity : FlutterActivity() {
    private val legacyMigrationExecutor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
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
        super.onDestroy()
    }

    private fun prepareLegacyRoomImport(): Map<String, Any?> {
        val source = getDatabasePath(LEGACY_ROOM_DB_NAME)
        if (!source.exists()) {
            return mapOf(
                "found" to false,
                "database_path" to null,
                "message" to "No legacy Room database found",
            )
        }

        val destination = File(
            File(filesDir, "getter-imports/legacy-room"),
            LEGACY_ROOM_DB_NAME,
        )
        copySqliteTriplet(source, destination)
        checkpointCopiedDatabase(destination)

        return mapOf(
            "found" to true,
            "database_path" to destination.absolutePath,
            "message" to "Legacy Room database prepared",
        )
    }

    private fun copySqliteTriplet(source: File, destination: File) {
        destination.parentFile?.mkdirs()
        SQLITE_SUFFIXES.forEach { suffix ->
            val sourceFile = File(source.path + suffix)
            val destinationFile = File(destination.path + suffix)
            if (sourceFile.exists()) {
                sourceFile.copyTo(destinationFile, overwrite = true)
            } else if (destinationFile.exists()) {
                destinationFile.delete()
            }
        }
    }

    private fun checkpointCopiedDatabase(database: File) {
        val db = SQLiteDatabase.openDatabase(
            database.path,
            null,
            SQLiteDatabase.OPEN_READWRITE,
        )
        try {
            db.rawQuery("PRAGMA wal_checkpoint(FULL)", null).use { cursor ->
                while (cursor.moveToNext()) {
                    // Drain the pragma result so SQLite performs the checkpoint.
                }
            }
            db.rawQuery("PRAGMA journal_mode=DELETE", null).use { cursor ->
                while (cursor.moveToNext()) {
                    // Drain the pragma result and leave a standalone import DB.
                }
            }
        } finally {
            db.close()
        }
        File(database.path + "-wal").delete()
        File(database.path + "-shm").delete()
    }

    private companion object {
        const val LEGACY_MIGRATION_CHANNEL = "net.xzos.upgradeall/legacy_migration"
        const val LEGACY_ROOM_DB_NAME = "app_metadata_database.db"
        val SQLITE_SUFFIXES = listOf("", "-wal", "-shm")
    }
}
