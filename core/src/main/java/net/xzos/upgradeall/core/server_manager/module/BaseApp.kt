package net.xzos.upgradeall.core.server_manager.module

import net.xzos.upgradeall.core.data.database.AppDatabase
import net.xzos.upgradeall.core.data_manager.AppDatabaseManager


abstract class BaseApp(private var database: AppDatabase) {
    val appDatabase: AppDatabase
        get() = AppDatabaseManager.getDatabase(database.id)?.apply {
            database = this
        } ?: database

    abstract suspend fun getUpdateStatus(): Int
}