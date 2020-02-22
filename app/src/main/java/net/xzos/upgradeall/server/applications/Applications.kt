package net.xzos.upgradeall.server.applications

import net.xzos.upgradeall.data_manager.database.AppDatabase
import net.xzos.upgradeall.server.applications.ApplicationsUtils
import net.xzos.upgradeall.server_manager.runtime.manager.module.app.App

class Applications(applicationDatabase: AppDatabase) {

    private val appSet = ApplicationsUtils(applicationDatabase).apps // 存储所有 APP 实体

    internal val apps: HashSet<App>
        get() = appSet

    fun delApp(app: App) {
        appSet.remove(app)
    }
}