package net.xzos.upgradeall.server_manager.runtime.manager

import net.xzos.upgradeall.data_manager.database.AppDatabase
import net.xzos.upgradeall.data_manager.database.manager.AppDatabaseManager
import net.xzos.upgradeall.server_manager.runtime.manager.module.app.App

object AppManager {

    private val singleAppSet = hashSetOf<App>() // 存储所有 APP 实体

    private val allAppSet = hashSetOf<App>()

    val apps: HashSet<App>
        get() = singleAppSet

    init {
        // 从数据库初始化 APP 实例
        initApp()
        // 同步数据
        allAppSet.addAll(singleAppSet)
    }

    private fun initApp() {
        val appDatabase = AppDatabaseManager.appDatabases
        for (appItem in appDatabase) {
            setApp(appItem)
        }
    }

    fun getSingleApp(databaseId: Long? = null, uuid: String? = null): App? {
        for (app in singleAppSet) {
            if ((uuid != null && app.appInfo.extraData?.cloudAppConfigGson?.uuid == uuid)
                    || (databaseId != null && app.appInfo.id == databaseId))
                return app
        }
        return null
    }

    private fun setApp(appDatabase: AppDatabase) {
        // 添加新的属性
        if (appDatabase.type != AppDatabase.APP_TYPE_TAG) {
            appDatabase.type = AppDatabase.APP_TYPE_TAG
            appDatabase.save()
        }
        singleAppSet.add(App(appDatabase))
    }

    fun addSingleApp(databaseId: Long? = null, app: App? = null): Boolean {
        // 具有查询重复功能（设计中一般不会有重复）
        if (databaseId != null) {
            val databaseIdList = singleAppSet.map {
                it.appInfo.id
            }
            if (!databaseIdList.contains(databaseId)) {
                AppDatabaseManager.getDatabase(databaseId = databaseId)?.run {
                    // 创建并添加应用
                    setApp(this)
                    return true
                }
            }
        } else if (app != null && app.appInfo.id != 0L) {
            // 尝试添加列表
            return singleAppSet.add(app)
        }
        return false
    }

    fun addApps(appList: HashSet<App>) {
        allAppSet.addAll(appList)
    }

    fun delSingleApp(app: App) {
        // TODO: initApp自维护，数据来源：独立 UI 数据
        singleAppSet.remove(app)
    }

    fun renewAppInHub(hubUuid: String) {
        for (app in singleAppSet) {
            if (app.appInfo.api_uuid == hubUuid) {
                app.renewEngine()
            }
        }
    }
}
