package net.xzos.upgradeall.core.server_manager.module

import net.xzos.upgradeall.core.data.database.AppDatabase


interface BaseApp {
    val appDatabase: AppDatabase
    var statusRenewedFun: (appStatus: Int) -> Unit

    suspend fun getUpdateStatus(): Int
}
