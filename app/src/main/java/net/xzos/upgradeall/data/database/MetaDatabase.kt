package net.xzos.upgradeall.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import net.xzos.upgradeall.application.MyApplication
import net.xzos.upgradeall.data.database.dao.AppDao
import net.xzos.upgradeall.data.database.dao.ApplicationsDao
import net.xzos.upgradeall.data.database.dao.HubDao
import net.xzos.upgradeall.data.database.table.AppEntity
import net.xzos.upgradeall.data.database.table.ApplicationsEntity
import net.xzos.upgradeall.data.database.table.HubEntity

@Database(entities = [AppEntity::class, ApplicationsEntity::class, HubEntity::class], version = 8)
@TypeConverters(Converters::class)
abstract class MetaDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
    abstract fun applicationsDao(): ApplicationsDao
    abstract fun hubDao(): HubDao
}

val metaDatabase = Room
        .databaseBuilder(MyApplication.context, MetaDatabase::class.java, "app_metadata_database.db")
        .addMigrations(MIGRATION_6_7)
        .addMigrations(MIGRATION_7_8)
        .build()
