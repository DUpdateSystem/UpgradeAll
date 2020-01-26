package net.xzos.upgradeAll.server.app.manager

import net.xzos.upgradeAll.data.database.manager.AppDatabaseManager
import net.xzos.upgradeAll.server.app.manager.module.App

internal object AppManager {

    private val appMap = mutableMapOf<Long, App>() // 存储 Updater Engine 数据

    init {
        initApp()
    }

    private fun initApp() {
        val repoDatabase = AppDatabaseManager.appDatabases
        for (updateItem in repoDatabase) {
            val appDatabaseId = updateItem.id
            setApp(appDatabaseId)
        }
    }

    fun getAppIds(): MutableSet<Long> =
            appMap.keys

    fun getApp(appDatabaseId: Long): App =
            appMap[appDatabaseId] ?: App(appDatabaseId).also { appMap[appDatabaseId] = it }

    fun setApp(appDatabaseId: Long) {
        appMap[appDatabaseId] = App(appDatabaseId)
    }

    fun delApp(appDatabaseId: Long) {
        // TODO: initApp自维护，数据来源：独立 UI 数据
        appMap.remove(appDatabaseId)
    }

    fun renewAppInHub(hubUuid: String) {
        val repoDatabases = AppDatabaseManager.getDatabaseList(hubUuid = hubUuid)
        for (repoDatabase in repoDatabases)
            repoDatabase?.let { setApp(it.id) }
    }
}
