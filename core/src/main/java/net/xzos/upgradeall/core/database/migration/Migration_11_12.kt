package net.xzos.upgradeall.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase


val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE hub ADD COLUMN applications_mode INT NOT NULL DEFAULT 0")
    }
}