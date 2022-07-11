package net.xzos.upgradeall.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SimpleSQLiteQuery
import kotlinx.coroutines.runBlocking
import net.xzos.upgradeall.core.database.dao.AppDao
import net.xzos.upgradeall.core.database.dao.ExtraAppDao
import net.xzos.upgradeall.core.database.dao.ExtraHubDao
import net.xzos.upgradeall.core.database.dao.HubDao
import net.xzos.upgradeall.core.database.migration.*
import net.xzos.upgradeall.core.database.table.AppEntity
import net.xzos.upgradeall.core.database.table.HubEntity
import net.xzos.upgradeall.core.database.table.extra_app.ExtraAppEntity
import net.xzos.upgradeall.core.database.table.extra_hub.ExtraHubEntity
import java.io.File

@Database(
    entities = [AppEntity::class, HubEntity::class, ExtraAppEntity::class, ExtraHubEntity::class],
    version = 16
)
@TypeConverters(Converters::class)
abstract class MetaDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
    abstract fun hubDao(): HubDao
    abstract fun extraAppDao(): ExtraAppDao
    abstract fun extraHubDao(): ExtraHubDao
}

suspend fun MetaDatabase.checkpoint() {
    val query = SimpleSQLiteQuery("pragma wal_checkpoint(full)")
    appDao().checkpoint(query)
    hubDao().checkpoint(query)
    extraAppDao().checkpoint(query)
    extraHubDao().checkpoint(query)
}

lateinit var metaDatabase: MetaDatabase
fun initDatabase(context: Context) {
    val dbName = "app_metadata_database.db"
    val dbFilePath = context.getDatabasePath(dbName).path
    if (File("$dbFilePath-wal").exists() || File("$dbFilePath-shm").exists())
        getDatabaseBuilder(
            context, MetaDatabase::class.java, dbName
        ).build().apply {
            runBlocking { this@apply.checkpoint() }
            this.close()
        }
    metaDatabase = getDatabase(context, MetaDatabase::class.java, dbName)
}

fun <E : RoomDatabase> getDatabaseBuilder(
    context: Context, less: Class<E>, name: String
): RoomDatabase.Builder<E> {
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
        .addMigrations(MIGRATION_15_16)
}

fun <E : RoomDatabase> getDatabase(context: Context, less: Class<E>, name: String) =
    getDatabaseBuilder(context, less, name)
        .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
        .build()