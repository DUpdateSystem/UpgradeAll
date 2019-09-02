package net.xzos.upgradeAll.server.app.manager

import net.xzos.upgradeAll.database.RepoDatabase
import net.xzos.upgradeAll.server.app.manager.api.App
import org.litepal.LitePal
import org.litepal.extension.findAll

class AppManager {

    private val appMap = mutableMapOf<Int, App>() // 存储 Updater Engine 数据


    private val appList: Collection<App>
        get() {
            return appMap.values
        }

    fun refreshAll(isAuto: Boolean) {
        if (appList.isEmpty())
            initApp()
        val appList = appList
        for (app in appList) {
            val updater = app.updater
            updater.renew(isAuto)
        }
    }

    private fun initApp() {
        val repoDatabase: List<RepoDatabase> = LitePal.findAll()
        for (updateItem in repoDatabase) {
            val appDatabaseId = updateItem.id
            setApp(appDatabaseId)
        }
    }

    fun getApp(appDatabaseId: Int): App {
        return appMap[appDatabaseId] ?: App(appDatabaseId)
    }

    fun setApp(appDatabaseId: Int) {
        appMap[appDatabaseId] = App(appDatabaseId)

    }

    fun delApp(appDatabaseId: Int) {
        // TODO: initApp自维护，数据来源：独立 UI 数据
        appMap.remove(appDatabaseId)
    }
}