package net.xzos.upgradeAll.server.app.manager

import net.xzos.upgradeAll.data.database.litepal.RepoDatabase
import net.xzos.upgradeAll.data.database.manager.AppDatabaseManager
import net.xzos.upgradeAll.server.app.manager.module.App

internal object AppManager {

    private val appSet = hashSetOf<App>() // 存储 Updater Engine 数据

    init {
        initApp()
    }

    private fun initApp() {
        val appDatabase = AppDatabaseManager.appDatabases
        for (appItem in appDatabase) {
            setApp(appItem)
        }
    }

    fun getApps(): HashSet<App> =
            appSet

    fun getApp(databaseId: Int? = null, uuid: String? = null): App? {
        for (app in appSet) {
            if ((uuid != null && app.appDatabase.extraData?.cloudAppConfig?.uuid == uuid)
                    || (databaseId != null && app.appDatabase.id == databaseId))
                return app
        }
        return null
    }

    fun addApp(app: App) {
        appSet.add(app)
    }

    fun setApp(appDatabase: RepoDatabase) {
        appSet.add(App(appDatabase))
    }

    fun delApp(app: App) {
        // TODO: initApp自维护，数据来源：独立 UI 数据
        appSet.remove(app)
    }

    fun renewAppInHub(hubUuid: String) {
        for (app in appSet) {
            if (app.appDatabase.api_uuid == hubUuid) {
                app.renewEngine()
            }
        }
    }
}
