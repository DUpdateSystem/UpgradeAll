package net.xzos.upgradeall.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import net.xzos.upgradeall.core.database.dao.AppDao
import net.xzos.upgradeall.core.database.dao.ExtraAppDao
import net.xzos.upgradeall.core.database.dao.HubDao
import net.xzos.upgradeall.core.database.migration.*
import net.xzos.upgradeall.core.database.table.AppEntity
import net.xzos.upgradeall.core.database.table.HubEntity
import net.xzos.upgradeall.core.database.table.extra_app.ExtraAppEntity

@Database(entities = [AppEntity::class, HubEntity::class, ExtraAppEntity::class], version = 15)
@TypeConverters(Converters::class)
abstract class MetaDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
    abstract fun hubDao(): HubDao
    abstract fun extraAppDao(): ExtraAppDao
}

lateinit var metaDatabase: MetaDatabase
fun initDatabase(context: Context) {
    metaDatabase = getDatabase(context, MetaDatabase::class.java, "app_metadata_database.db")
}

fun <E : RoomDatabase> getDatabase(context: Context, less: Class<E>, name: String): E {
    return Room
        .databaseBuilder(context, less, name)
        .addMigrations(MIGRATION_6_7)
        .addMigrations(MIGRATION_7_8)
        .addMigrations(MIGRATION_8_9)
        .addMigrations(MIGRATION_9_10)
        .addMigrations(MIGRATION_8_10)
        .addMigrations(MIGRATION_10_11)
        .addMigrations(MIGRATION_11_12)
        .addMigrations(MIGRATION_12_13)
        .addMigrations(MIGRATION_13_14)
        .addMigrations(MIGRATION_14_15)
        .build()
}