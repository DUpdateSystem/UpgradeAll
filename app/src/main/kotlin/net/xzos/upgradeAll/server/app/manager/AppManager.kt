package net.xzos.upgradeAll.server.app.manager

import net.xzos.upgradeAll.database.RepoDatabase
import net.xzos.upgradeAll.server.app.manager.module.App
import org.litepal.LitePal
import org.litepal.extension.find
import org.litepal.extension.findAll

internal class AppManager {

    init {
        initApp()
    }

    private fun initApp() {
        val repoDatabase: List<RepoDatabase> = LitePal.findAll()
        for (updateItem in repoDatabase) {
            val appDatabaseId = updateItem.id
            setApp(appDatabaseId)
        }
    }

    fun getApp(appDatabaseId: Long): App {
        return appMap[appDatabaseId] ?: App(appDatabaseId)
    }

    fun setApp(appDatabaseId: Long) {
        appMap[appDatabaseId] = App(appDatabaseId)

    }

    fun delApp(appDatabaseId: Long) {
        // TODO: initApp自维护，数据来源：独立 UI 数据
        getApp(appDatabaseId).engineExit()
        appMap.remove(appDatabaseId)
    }

    fun renewAppInHub(hubUuid: String) {
        val repoDatabases: List<RepoDatabase> = LitePal.where("api_uuid = ?", hubUuid).find()
        for(repoDatabase in repoDatabases)
            setApp(repoDatabase.id)
    }

    companion object {
        private val appMap = mutableMapOf<Long, App>() // 存储 Updater Engine 数据

    }
}