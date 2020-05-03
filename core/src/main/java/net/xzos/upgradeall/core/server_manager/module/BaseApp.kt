package net.xzos.upgradeall.core.server_manager.module

import net.xzos.upgradeall.core.data.database.AppDatabase


interface BaseApp {
    val appDatabase: AppDatabase

    suspend fun getUpdateStatus(): Int
}