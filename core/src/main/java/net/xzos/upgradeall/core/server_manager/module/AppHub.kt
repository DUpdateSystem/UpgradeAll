package net.xzos.upgradeall.core.server_manager.module

interface AppHub {
    suspend fun getAppUpdateStatus(baseApp: BaseApp): Int
}