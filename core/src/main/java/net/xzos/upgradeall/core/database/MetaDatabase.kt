package net.xzos.upgradeall.core.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import net.xzos.upgradeall.core.coreConfig
import net.xzos.upgradeall.core.database.dao.AppDao
import net.xzos.upgradeall.core.database.dao.HubDao
import net.xzos.upgradeall.core.database.table.AppEntity
import net.xzos.upgradeall.core.database.table.HubEntity

@Database(entities = [AppEntity::class, HubEntity::class], version = 9)
@TypeConverters(Converters::class)
abstract class MetaDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
    abstract fun hubDao(): HubDao
}

val metaDatabase = Room
    .databaseBuilder(
        coreConfig.androidContext,
        MetaDatabase::class.java,
        "app_metadata_database.db"
    )
    .addMigrations(MIGRATION_6_7)
    .addMigrations(MIGRATION_7_8)
    .build()
