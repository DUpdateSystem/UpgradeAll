package net.xzos.upgradeall.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase


val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("DROP INDEX applications_key_value")
        database.execSQL("""
           CREATE UNIQUE INDEX applications_key_value
           on applications (hub_uuid, extra_id, auth); 
        """
        )
    }
}