package net.xzos.upgradeall.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase


val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE extra_app
            (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            app_id TEXT NOT NULL,
            mark_version_number TEXT DEFAULT null
            );
        """
        )
    }
}