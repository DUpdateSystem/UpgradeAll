package net.xzos.upgradeall.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase


val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE hub ADD COLUMN sort_point INT NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE app ADD COLUMN enable_hub_list TEXT DEFAULT NULL")
    }
}