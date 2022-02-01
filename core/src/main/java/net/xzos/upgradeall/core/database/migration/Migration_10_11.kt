package net.xzos.upgradeall.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase


val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE app ADD COLUMN invalid_version_number_field_regex TEXT DEFAULT null")
    }
}