package net.xzos.upgradeall

import android.database.sqlite.SQLiteDatabase
import java.io.File

internal data class PreparedLegacyRoomImport(
    val found: Boolean,
    val databasePath: String?,
    val message: String,
) {
    fun toMethodChannelResult(): Map<String, Any?> = mapOf(
        "found" to found,
        "database_path" to databasePath,
        "message" to message,
    )
}

internal fun interface CopiedDatabaseCheckpointer {
    fun checkpoint(database: File)
}

internal class LegacyRoomImportPreparer(
    private val checkpointer: CopiedDatabaseCheckpointer = AndroidSqliteCopiedDatabaseCheckpointer(),
) {
    fun prepare(source: File, destination: File): PreparedLegacyRoomImport {
        if (!source.exists()) {
            return PreparedLegacyRoomImport(
                found = false,
                databasePath = null,
                message = "No legacy Room database found",
            )
        }

        copySqliteTriplet(source, destination)
        checkpointer.checkpoint(destination)

        return PreparedLegacyRoomImport(
            found = true,
            databasePath = destination.absolutePath,
            message = "Legacy Room database prepared",
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

    private companion object {
        val SQLITE_SUFFIXES = listOf("", "-wal", "-shm")
    }
}

internal class AndroidSqliteCopiedDatabaseCheckpointer : CopiedDatabaseCheckpointer {
    override fun checkpoint(database: File) {
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
}
