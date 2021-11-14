package net.xzos.upgradeall.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase


val MIGRATION_15_16 = object : Migration(15, 16) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE extra_hub
            (
            id TEXT PRIMARY KEY AUTOINCREMENT NOT NULL,
            enable_global BOOLEAN NOT NULL,
            url_replace_search TEXT DEFAULT null,
            url_replace_string TEXT DEFAULT null
            );
        """
        )
    }
}