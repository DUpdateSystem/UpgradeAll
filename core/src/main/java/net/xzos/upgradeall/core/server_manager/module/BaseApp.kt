package net.xzos.upgradeall.core.server_manager.module

import net.xzos.upgradeall.core.data.database.BaseAppDatabase


interface BaseApp {
    val appDatabase: BaseAppDatabase
    var statusRenewedFun: (appStatus: Int) -> Unit

    suspend fun getUpdateStatus(): Int
    fun refreshData()
}
